package ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import model.Process;
import model.Burst;
import scheduler.*;
import memory.*;
import io.IOManager;
import simulation.SimulationController;
import config.ProcessConfigParser;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Interfaz grafica principal del simulador de Sistema Operativo
 * Muestra en tiempo real el estado de CPU, memoria, E/S y métricas
 */
public class SimulatorGUI extends Application {
    
    // Componentes de la simulación
    private SimulationController controller;
    private Thread simulationThread;
    private volatile boolean simulationRunning = false;
    private volatile boolean simulationPaused = false;
    private Process currentProcess = null;
    private int quantumRemaining = 0;
    private List<Process> allProcesses = new ArrayList<>();
    private int totalCPUTime = 0;
    private int idleTime = 0;
    
    // Controles de configuracion
    private ComboBox<String> schedulerCombo;
    private ComboBox<String> memoryAlgorithmCombo;
    private Spinner<Integer> quantumSpinner;
    private Spinner<Integer> memoryFramesSpinner;
    private Spinner<Integer> simulationSpeedSpinner;
    
    // Areas de visualizacion
    private TextArea processListArea;
    private TextArea cpuStateArea;
    private TextArea memoryStateArea;
    private TextArea ioStateArea;
    private TextArea ganttArea;
    private TextArea metricsArea;
    private TextArea logArea;
    
    // Visualización grafica de memoria
    private GridPane memoryGrid;
    private Label[] memoryFrameLabels;
    
    // Visualización de colas
    private TextArea readyQueueArea;
    private TextArea blockedQueueArea;
    
    // Botones de control
    private Button startButton;
    private Button pauseButton;
    private Button stopButton;
    private Button resetButton;
    private Button loadFileButton;
    private Button addProcessButton;
    
    // Estado actual
    private Label timeLabel;
    private Label cpuUtilLabel;
    private Label pageFaultsLabel;
    private ObservableList<Process> processList = FXCollections.observableArrayList();
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Simulador de Sistema Operativo - Gestión de Procesos y Memoria Virtual");
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Panel superior: controles de configuracion
        root.setTop(createConfigPanel());
        
        // Panel central: visualizacion principal
        root.setCenter(createMainPanel());
        
        // Panel inferior: controles de simulacion
        root.setBottom(createControlPanel());
        
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            stopSimulation();
            Platform.exit();
        });
        
        primaryStage.show();
        
        // Log inicial
        appendLog("Sistema iniciado. Configure los parametros y cargue procesos.");
    }
    
    /**
     * Crea el panel de configuracion superior
     */
    private VBox createConfigPanel() {
        VBox configBox = new VBox(10);
        configBox.setPadding(new Insets(10));
        configBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1;");
        
        Label titleLabel = new Label("CONFIGURACIÓN DEL SIMULADOR");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // Primera fila: Algoritmos
        HBox row1 = new HBox(15);
        row1.setAlignment(Pos.CENTER_LEFT);
        
        Label schedLabel = new Label("Planificación:");
        schedulerCombo = new ComboBox<>();
        schedulerCombo.getItems().addAll("FCFS", "SJF", "Round Robin");
        schedulerCombo.setValue("Round Robin");
        schedulerCombo.setPrefWidth(150);
        
        Label memLabel = new Label("Reemplazo de Paginas:");
        memoryAlgorithmCombo = new ComboBox<>();
        memoryAlgorithmCombo.getItems().addAll("FIFO", "LRU", "Optimal");
        memoryAlgorithmCombo.setValue("LRU");
        memoryAlgorithmCombo.setPrefWidth(150);
        
        row1.getChildren().addAll(schedLabel, schedulerCombo, memLabel, memoryAlgorithmCombo);
        
        // Segunda fila: Parametros numericos
        HBox row2 = new HBox(15);
        row2.setAlignment(Pos.CENTER_LEFT);
        
        Label quantumLabel = new Label("Quantum:");
        quantumSpinner = new Spinner<>(1, 10, 3);
        quantumSpinner.setPrefWidth(80);
        quantumSpinner.setDisable(!schedulerCombo.getValue().equals("Round Robin"));
        
        Label framesLabel = new Label("Marcos de Memoria:");
        memoryFramesSpinner = new Spinner<>(5, 30, 10);
        memoryFramesSpinner.setPrefWidth(80);
        
        Label speedLabel = new Label("Velocidad (ms):");
        simulationSpeedSpinner = new Spinner<>(100, 2000, 500);
        simulationSpeedSpinner.setPrefWidth(80);
        
        row2.getChildren().addAll(quantumLabel, quantumSpinner, framesLabel, 
                                   memoryFramesSpinner, speedLabel, simulationSpeedSpinner);
        
        // Habilitar/deshabilitar quantum según scheduler
        schedulerCombo.setOnAction(e -> {
            boolean isRR = schedulerCombo.getValue().equals("Round Robin");
            quantumSpinner.setDisable(!isRR);
        });
        
        configBox.getChildren().addAll(titleLabel, row1, row2);
        
        return configBox;
    }
    
    /**
     * Crea el panel principal con tabs de visualización
     */
    private Node createMainPanel() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Tab 1: Vista general
        Tab overviewTab = new Tab("Vista General");
        overviewTab.setContent(createOverviewPanel());
        
        // Tab 2: Procesos
        Tab processesTab = new Tab("Procesos");
        processesTab.setContent(createProcessesPanel());
        
        // Tab 3: Memoria
        Tab memoryTab = new Tab("Memoria");
        memoryTab.setContent(createMemoryPanel());
        
        // Tab 4: Diagrama de Gantt
        Tab ganttTab = new Tab("Diagrama de Gantt");
        ganttTab.setContent(createGanttPanel());
        
        //Tab 5:Metricas
        Tab metricsTab = new Tab("Metricas de Desempenio");
        metricsTab.setContent(createMetricsPanel());
        
        // Tab 6: Log de eventos
        Tab logTab = new Tab("Log de Eventos");
        logTab.setContent(createLogPanel());
        
        tabPane.getTabs().addAll(overviewTab, processesTab, memoryTab, ganttTab, metricsTab, logTab);
        
        return tabPane;
    }
    
    /**
     * Vista general con estado de CPU, colas y resumen
     */
    private Node createOverviewPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        // Estado del sistema (columna 0, fila 0-2)
        VBox statusBox = createBoxWithTitle("Estado del Sistema");
        timeLabel = new Label("Tiempo: 0");
        timeLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        cpuUtilLabel = new Label("Utilización CPU: 0%");
        pageFaultsLabel = new Label("Fallos de Pagina: 0");
        
        statusBox.getChildren().addAll(timeLabel, cpuUtilLabel, pageFaultsLabel);
        grid.add(statusBox, 0, 0);
        
        // Estado de CPU (columna 0, fila 1)
        VBox cpuBox = createBoxWithTitle("Estado de CPU");
        cpuStateArea = new TextArea("CPU IDLE");
        cpuStateArea.setPrefRowCount(5);
        cpuStateArea.setEditable(false);
        cpuStateArea.setStyle("-fx-font-family: monospaced;");
        cpuBox.getChildren().add(cpuStateArea);
        grid.add(cpuBox, 0, 1);
        
        // Cola de listos (columna 1, fila 0)
        VBox readyBox = createBoxWithTitle("Cola de Listos");
        readyQueueArea = new TextArea("Vacía");
        readyQueueArea.setPrefRowCount(8);
        readyQueueArea.setEditable(false);
        readyQueueArea.setStyle("-fx-font-family: monospaced;");
        readyBox.getChildren().add(readyQueueArea);
        grid.add(readyBox, 1, 0, 1, 2);
        
        // Cola de bloqueados (columna 2, fila 0)
        VBox blockedBox = createBoxWithTitle("Procesos Bloqueados");
        blockedQueueArea = new TextArea("Ninguno");
        blockedQueueArea.setPrefRowCount(8);
        blockedQueueArea.setEditable(false);
        blockedQueueArea.setStyle("-fx-font-family: monospaced;");
        blockedBox.getChildren().add(blockedQueueArea);
        grid.add(blockedBox, 2, 0, 1, 2);
        
        // Estado de E/S (columna 0-2, fila 2)
        VBox ioBox = createBoxWithTitle("Estado de Operaciones de E/S");
        ioStateArea = new TextArea("No hay operaciones activas");
        ioStateArea.setPrefRowCount(4);
        ioStateArea.setEditable(false);
        ioStateArea.setStyle("-fx-font-family: monospaced;");
        ioBox.getChildren().add(ioStateArea);
        grid.add(ioBox, 0, 2, 3, 1);
        
        // Configurar columnas para que se expandan
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(33);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(34);
        grid.getColumnConstraints().addAll(col1, col2, col3);
        
        return new ScrollPane(grid);
    }
    
    /**
     * Panel de procesos con lista y gestión
     */
    private Node createProcessesPanel() {
        BorderPane pane = new BorderPane();
        pane.setPadding(new Insets(10));
        
        // Lista de procesos
        VBox listBox = createBoxWithTitle("Lista de Procesos");
        processListArea = new TextArea();
        processListArea.setEditable(false);
        processListArea.setStyle("-fx-font-family: monospaced;");
        listBox.getChildren().add(processListArea);
        
        pane.setCenter(listBox);
        
        // Botones de gestión
        VBox buttonBox = new VBox(10);
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.TOP_CENTER);
        
        loadFileButton = new Button("Cargar desde Archivo");
        loadFileButton.setMaxWidth(Double.MAX_VALUE);
        loadFileButton.setOnAction(e -> loadProcessesFromFile());
        
        addProcessButton = new Button("Agregar Proceso Manual");
        addProcessButton.setMaxWidth(Double.MAX_VALUE);
        addProcessButton.setOnAction(e -> showAddProcessDialog());
        
        Button exampleButton = new Button("Cargar Ejemplo");
        exampleButton.setMaxWidth(Double.MAX_VALUE);
        exampleButton.setOnAction(e -> loadExampleProcesses());
        
        Button clearButton = new Button("Limpiar Procesos");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.setOnAction(e -> clearProcesses());
        
        buttonBox.getChildren().addAll(loadFileButton, addProcessButton, exampleButton, clearButton);
        pane.setRight(buttonBox);
        
        return pane;
    }
    
    /**
     * Panel de memoria con visualización grafica
     */
    private Node createMemoryPanel() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        
        // Estado textual
        VBox stateBox = createBoxWithTitle("Estado de Memoria");
        memoryStateArea = new TextArea();
        memoryStateArea.setPrefRowCount(8);
        memoryStateArea.setEditable(false);
        memoryStateArea.setStyle("-fx-font-family: monospaced;");
        stateBox.getChildren().add(memoryStateArea);
        
        // Visualización grafica de marcos
        VBox gridBox = createBoxWithTitle("Marcos de Memoria (Visualización Grafica)");
        ScrollPane scrollPane = new ScrollPane();
        memoryGrid = new GridPane();
        memoryGrid.setHgap(5);
        memoryGrid.setVgap(5);
        memoryGrid.setPadding(new Insets(10));
        scrollPane.setContent(memoryGrid);
        scrollPane.setFitToWidth(true);
        gridBox.getChildren().add(scrollPane);
        
        vbox.getChildren().addAll(stateBox, gridBox);
        VBox.setVgrow(gridBox, Priority.ALWAYS);
        
        return vbox;
    }
    
    /**
     * Panel de Diagrama de Gantt
     */
    private Node createGanttPanel() {
        VBox vbox = createBoxWithTitle("Diagrama de Gantt - Ejecución de Procesos");
        ganttArea = new TextArea();
        ganttArea.setEditable(false);
        ganttArea.setStyle("-fx-font-family: monospaced; -fx-font-size: 11;");
        vbox.getChildren().add(ganttArea);
        VBox.setVgrow(ganttArea, Priority.ALWAYS);
        
        return vbox;
    }
    
    /**
     * Panel de métricas de desempeño
     */
    private Node createMetricsPanel() {
        VBox vbox = createBoxWithTitle("Métricas de Desempeño del Sistema");
        metricsArea = new TextArea();
        metricsArea.setEditable(false);
        metricsArea.setStyle("-fx-font-family: monospaced;");
        vbox.getChildren().add(metricsArea);
        VBox.setVgrow(metricsArea, Priority.ALWAYS);
        
        return vbox;
    }
    
    /**
     * Panel de log de eventos
     */
    private Node createLogPanel() {
        VBox vbox = createBoxWithTitle("Log de Eventos del Sistema");
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: monospaced;");
        logArea.setWrapText(true);
        vbox.getChildren().add(logArea);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        
        return vbox;
    }
    
    /**
     * Crea el panel de controles de simulación
     */
    private HBox createControlPanel() {
        HBox controlBox = new HBox(10);
        controlBox.setPadding(new Insets(10));
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1;");
        
        startButton = new Button("Iniciar Simulación");
        startButton.setStyle("-fx-font-size: 14; -fx-padding: 10;");
        startButton.setOnAction(e -> startSimulation());
        
        pauseButton = new Button("Pausar");
        pauseButton.setStyle("-fx-font-size: 14; -fx-padding: 10;");
        pauseButton.setDisable(true);
        pauseButton.setOnAction(e -> pauseSimulation());
        
        stopButton = new Button("Detener");
        stopButton.setStyle("-fx-font-size: 14; -fx-padding: 10;");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopSimulation());
        
        resetButton = new Button("Reiniciar");
        resetButton.setStyle("-fx-font-size: 14; -fx-padding: 10;");
        resetButton.setOnAction(e -> resetSimulation());
        
        controlBox.getChildren().addAll(startButton, pauseButton, stopButton, resetButton);
        
        return controlBox;
    }
    
    /**
     * Crea un VBox con título
     */
    private VBox createBoxWithTitle(String title) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));
        box.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-color: white;");
        
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        titleLabel.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 5;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        
        box.getChildren().add(titleLabel);
        
        return box;
    }
    
    /**
     * Inicia la simulación
     */
    private void startSimulation() {
        if (processList.isEmpty()) {
            showAlert("No hay procesos", "Por favor cargue al menos un proceso antes de iniciar la simulación.");
            return;
        }
        
        // Crear componentes del simulador
        String schedulerType = schedulerCombo.getValue();
        int quantum = quantumSpinner.getValue();
        SchedulingAlgorithm scheduler = createScheduler(schedulerType, quantum);
        
        String memAlg = memoryAlgorithmCombo.getValue();
        int frames = memoryFramesSpinner.getValue();
        PageReplacementAlgorithm pageAlgorithm = createPageAlgorithm(memAlg);
        MemoryManager memoryManager = new MemoryManager(frames, pageAlgorithm);
        IOManager ioManager = new IOManager();
        
        controller = new SimulationController(scheduler, memoryManager, ioManager, quantum, 500);
        
        // Agregar procesos clonados
        allProcesses = cloneProcesses(new ArrayList<>(processList));
        controller.addProcesses(allProcesses);
        
        // Resetear variables de estado
        currentProcess = null;
        quantumRemaining = 0;
        totalCPUTime = 0;
        idleTime = 0;
        
        // Inicializar visualización de memoria
        initializeMemoryGrid(frames);
        
        // Iniciar thread de simulación
        simulationRunning = true;
        simulationPaused = false;
        simulationThread = new Thread(this::runSimulationLoop);
        simulationThread.setDaemon(true);
        simulationThread.start();
        
        // Actualizar botones
        startButton.setDisable(true);
        pauseButton.setDisable(false);
        stopButton.setDisable(false);
        
        appendLog("Simulación iniciada con " + schedulerType + " + " + memAlg);
    }
    
    /**
     * Loop principal de simulación (ejecuta en thread separado)
     */
    private void runSimulationLoop() {
        SimulationClock.reset();
        int delay = simulationSpeedSpinner.getValue();
        
        while (simulationRunning && SimulationClock.getTime() < 500) {
            if (!simulationPaused) {
                // Ejecutar un paso de simulación
                executeSimulationStep();
                
                // Actualizar UI en el thread de JavaFX
                Platform.runLater(this::updateUI);
                
                // Esperar según velocidad configurada
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        
        // Simulación terminada
        Platform.runLater(() -> {
            simulationRunning = false;
            startButton.setDisable(false);
            pauseButton.setDisable(true);
            stopButton.setDisable(true);
            appendLog("Simulación finalizada.");
            updateFinalMetrics();
        });
    }
    
    /**
     * Ejecuta un paso de la simulación
     */
    private void executeSimulationStep() {
        if (controller == null) return;
        
        int currentTime = SimulationClock.getTime();
        
        // 1. Verificar llegada de nuevos procesos
        checkNewArrivals(currentTime);
        
        // 2. Actualizar operaciones de E/S
        List<Process> completedIO = controller.getIOManager().updateIOOperations(allProcesses);
        for (Process p : completedIO) {
            // Proceso completó E/S, agregarlo a cola de listos
            // (IOManager ya cambió el estado a READY)
            controller.getScheduler().addProcess(p);
            controller.getGanttChart().addEvent(currentTime, p.getPid() + " E/S completada");
            final String pid = p.getPid();
            Platform.runLater(() -> appendLog(pid + " completó E/S y vuelve a cola"));
        }
        
        // 3. Seleccionar proceso a ejecutar si no hay uno
        if (currentProcess == null || currentProcess.getState() != Process.ProcessState.RUNNING) {
            currentProcess = controller.getScheduler().getNextProcess();
            
            if (currentProcess != null) {
                // Preparar proceso: cargar paginas necesarias
                boolean ready = prepareProcessForExecution(currentProcess);
                
                if (ready) {
                    currentProcess.setState(Process.ProcessState.RUNNING);
                    
                    if (currentProcess.getFirstExecutionTime() == -1) {
                        currentProcess.setFirstExecutionTime(currentTime);
                        controller.getScheduler().onProcessStarted(currentProcess);
                    }
                    
                    quantumRemaining = controller.getScheduler().isPreemptive() ? 
                        quantumSpinner.getValue() : Integer.MAX_VALUE;
                    
                    final String pid = currentProcess.getPid();
                    Platform.runLater(() -> appendLog("CPU ejecutando: " + pid));
                } else {
                    currentProcess = null;
                }
            }
        }
        
        // 4. Ejecutar proceso actual
        if (currentProcess != null && currentProcess.getState() == Process.ProcessState.RUNNING) {
            Burst currentBurst = currentProcess.getCurrentBurst();
            
            if (currentBurst != null) {
                if (currentBurst.getType() == Burst.BurstType.IO) {
                    // Bloquear por E/S
                    int ioDuration = currentBurst.getDuration();
                    currentProcess.setState(Process.ProcessState.BLOCKED_IO);
                    controller.getIOManager().startIOOperation(currentProcess, ioDuration);
                    currentProcess.completeCurrentBurst();
                    controller.getGanttChart().addEvent(currentTime, currentProcess.getPid() + " -> E/S");
                    final String pid = currentProcess.getPid();
                    Platform.runLater(() -> appendLog(pid + " bloqueado por E/S"));
                    currentProcess = null;
                    quantumRemaining = 0;
                    
                } else if (currentBurst.getType() == Burst.BurstType.CPU) {
                    // Ejecutar CPU
                    int timeToExecute = Math.min(quantumRemaining, currentBurst.getRemainingTime());
                    timeToExecute = Math.min(timeToExecute, 1); // 1 unidad a la vez
                    
                    int executed = currentProcess.executeBurst(timeToExecute);
                    quantumRemaining -= executed;
                    totalCPUTime += executed;
                    
                    controller.getScheduler().recordCPUExecution(currentProcess, executed);
                    controller.getMemoryManager().notifyProcessCPUUsage(currentProcess, executed);
                    controller.getGanttChart().addExecution(currentProcess.getPid(), currentTime, currentTime + executed);
                    
                    // Verificar si completó la rafaga
                    if (currentBurst.getRemainingTime() <= 0) {
                        if (currentProcess.isCompleted()) {
                            // Proceso terminado
                            currentProcess.setCompletionTime(currentTime + 1);
                            currentProcess.setState(Process.ProcessState.TERMINATED);
                            controller.getScheduler().onProcessCompletion(currentProcess);
                            controller.getMemoryManager().freePagesForProcess(currentProcess);
                            controller.getGanttChart().addEvent(currentTime + 1, currentProcess.getPid() + " TERMINADO");
                            final String pid = currentProcess.getPid();
                            Platform.runLater(() -> appendLog(pid + " ha terminado"));
                            currentProcess = null;
                            quantumRemaining = 0;
                        } else {
                            // Verificar siguiente rafaga
                            Burst nextBurst = currentProcess.getCurrentBurst();
                            if (nextBurst != null && nextBurst.getType() == Burst.BurstType.IO) {
                                // Siguiente es E/S, bloquear inmediatamente
                                int ioDuration = nextBurst.getDuration();
                                currentProcess.setState(Process.ProcessState.BLOCKED_IO);
                                controller.getIOManager().startIOOperation(currentProcess, ioDuration);
                                currentProcess.completeCurrentBurst();
                                controller.getGanttChart().addEvent(currentTime + 1, currentProcess.getPid() + " -> E/S");
                                final String pid = currentProcess.getPid();
                                Platform.runLater(() -> appendLog(pid + " bloqueado por E/S"));
                                currentProcess = null;
                                quantumRemaining = 0;
                            }
                        }
                    } else if (quantumRemaining <= 0 && controller.getScheduler().isPreemptive()) {
                        // Quantum agotado
                        currentProcess.setState(Process.ProcessState.READY);
                        controller.getScheduler().onProcessInterrupted(currentProcess);
                        controller.getGanttChart().addEvent(currentTime, currentProcess.getPid() + " QUANTUM");
                        final String pid = currentProcess.getPid();
                        Platform.runLater(() -> appendLog(pid + " quantum agotado"));
                        currentProcess = null;
                        quantumRemaining = 0;
                    }
                }
            }
        } else {
            // CPU idle
            idleTime++;
            controller.getGanttChart().addExecution("IDLE", currentTime, currentTime + 1);
            
            // Debug: Ver por qué esta idle cada 20 unidades
            if (currentTime % 20 == 0 && currentTime > 0) {
                int readyCount = controller.getScheduler().getReadyQueue().size();
                int ioBlocked = 0;
                int terminated = 0;
                for (Process p : allProcesses) {
                    if (p.getState() == Process.ProcessState.BLOCKED_IO) ioBlocked++;
                    if (p.getState() == Process.ProcessState.TERMINATED) terminated++;
                }
                final String debugMsg = String.format("t=%d: IDLE - Listos:%d, E/S:%d, Terminados:%d", 
                    currentTime, readyCount, ioBlocked, terminated);
                Platform.runLater(() -> appendLog(debugMsg));
            }
        }
        
        // 5. Avanzar el reloj
        SimulationClock.incrementTime();
        
        // 6. Actualizar métricas periódicamente (cada 10 unidades)
        if (currentTime % 10 == 0 && currentTime > 0) {
            Platform.runLater(() -> updateFinalMetrics());
        }
        
        // 7. Verificar si todos terminaron
        if (allProcessesCompleted()) {
            simulationRunning = false;
            Platform.runLater(() -> {
                appendLog("Todos los procesos completados");
            });
        }
    }
    
    private void checkNewArrivals(int currentTime) {
        for (Process p : allProcesses) {
            if (p.getArrivalTime() == currentTime && p.getState() == Process.ProcessState.NEW) {
                p.setState(Process.ProcessState.READY);
                controller.getScheduler().addProcess(p);
                controller.getGanttChart().addEvent(currentTime, p.getPid() + " LLEGA");
                final String pid = p.getPid();
                Platform.runLater(() -> appendLog(pid + " llegó al sistema"));
            }
        }
    }
    
    private boolean prepareProcessForExecution(Process p) {
        controller.getMemoryManager().loadPagesForProcess(p);
        return true;
    }
    
    private boolean allProcessesCompleted() {
        for (Process p : allProcesses) {
            if (p.getState() != Process.ProcessState.TERMINATED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Actualiza toda la interfaz
     */
    private void updateUI() {
        updateSystemStatus();
        updateQueues();
        updateCPUState();
        updateMemoryState();
        updateIOState();
        updateGantt();
    }
    
    /**
     * Actualiza estado del sistema
     */
    private void updateSystemStatus() {
        int currentTime = SimulationClock.getTime();
        timeLabel.setText("Tiempo: " + currentTime);
        
        if (controller != null) {
            // Calcular utilización de CPU
            double utilization = 0.0;
            if (currentTime > 0) {
                utilization = (totalCPUTime * 100.0) / currentTime;
            }
            cpuUtilLabel.setText(String.format("Utilización CPU: %.1f%%", utilization));
            pageFaultsLabel.setText("Fallos de Pagina: " + 
                controller.getMemoryManager().getPageFaults());
        }
    }
    
    /**
     * Actualiza colas de procesos
     */
    private void updateQueues() {
        if (controller != null && allProcesses != null) {
            List<Process> ready = controller.getScheduler().getReadyQueue();
            StringBuilder readyText = new StringBuilder();
            if (ready.isEmpty()) {
                readyText.append("Cola vacía\n");
            } else {
                readyText.append(String.format("Procesos en cola: %d\n\n", ready.size()));
                for (Process p : ready) {
                    readyText.append(String.format("%s - CPU restante: %d, Estado: %s\n", 
                        p.getPid(), p.getRemainingCPUTime(), p.getState()));
                }
            }
            readyQueueArea.setText(readyText.toString());
            
            // Bloqueados
            StringBuilder blockedText = new StringBuilder();
            int blockedCount = 0;
            for (Process p : allProcesses) {
                if (p.getState() == Process.ProcessState.BLOCKED_MEMORY || 
                    p.getState() == Process.ProcessState.BLOCKED_IO) {
                    blockedText.append(String.format("%s - Estado: %s\n", 
                        p.getPid(), p.getState()));
                    blockedCount++;
                }
            }
            if (blockedCount == 0) {
                blockedText.append("Ningún proceso bloqueado");
            } else {
                blockedText.insert(0, String.format("Procesos bloqueados: %d\n\n", blockedCount));
            }
            blockedQueueArea.setText(blockedText.toString());
        }
    }
    
    /**
     * Actualiza estado de CPU
     */
    private void updateCPUState() {
        if (controller != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== ESTADO DE CPU ===\n\n");
            
            // Mostrar proceso en ejecución
            if (currentProcess != null && currentProcess.getState() == Process.ProcessState.RUNNING) {
                sb.append(String.format("Ejecutando: %s\n", currentProcess.getPid()));
                sb.append(String.format("CPU restante: %d\n", currentProcess.getRemainingCPUTime()));
                if (controller.getScheduler().isPreemptive()) {
                    sb.append(String.format("Quantum restante: %d\n", quantumRemaining));
                }
            } else {
                sb.append("CPU: IDLE (inactiva)\n");
            }
            
            sb.append(String.format("\nTiempo actual: %d\n", SimulationClock.getTime()));
            
            cpuStateArea.setText(sb.toString());
        }
    }
    
    /**
     * Actualiza estado de memoria
     */
    private void updateMemoryState() {
        if (controller != null) {
            memoryStateArea.setText(controller.getMemoryManager().getMemoryState());
            updateMemoryGridVisual();
        }
    }
    
    /**
     * Actualiza visualización grafica de memoria
     */
    private void updateMemoryGridVisual() {
        if (memoryFrameLabels == null || controller == null) return;
        
        // Por ahora solo actualizamos con información basica
        // La información detallada se muestra en memoryStateArea
        int totalFrames = controller.getMemoryManager().getTotalFrames();
        int occupiedFrames = controller.getMemoryManager().getOccupiedFrameCount();
        
        // Actualizar grid con colores basicos según ocupación
        for (int i = 0; i < memoryFrameLabels.length && i < totalFrames; i++) {
            Label label = memoryFrameLabels[i];
            
            // Aproximación simple: primeros frames ocupados
            if (i < occupiedFrames) {
                label.setStyle("-fx-border-color: black; -fx-background-color: lightblue;");
            } else {
                label.setStyle("-fx-border-color: black; -fx-background-color: lightgray;");
            }
        }
    }
    
    /**
     * Actualiza estado de E/S
     */
    private void updateIOState() {
        if (controller != null) {
            ioStateArea.setText(controller.getIOManager().getIOState());
        }
    }
    
    /**
     * Actualiza diagrama de Gantt
     */
    private void updateGantt() {
        if (controller != null) {
            ganttArea.setText(controller.getGanttChart().toString());
        }
    }
    
    /**
     * Actualiza métricas finales
     */
    private void updateFinalMetrics() {
        if (controller != null) {
            StringBuilder metrics = new StringBuilder();
            metrics.append(controller.getScheduler().getMetrics()).append("\n\n");
            metrics.append(controller.getMemoryManager().getMemoryMetrics()).append("\n\n");
            metrics.append(controller.getIOManager().getIOMetrics());
            metricsArea.setText(metrics.toString());
        }
    }
    
    /**
     * Pausa la simulación
     */
    private void pauseSimulation() {
        simulationPaused = !simulationPaused;
        pauseButton.setText(simulationPaused ? "Continuar" : "Pausar");
        appendLog(simulationPaused ? "Simulación pausada" : "Simulación reanudada");
    }
    
    /**
     * Detiene la simulación
     */
    private void stopSimulation() {
        simulationRunning = false;
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
        appendLog("Simulación detenida por el usuario.");
    }
    
    /**
     * Reinicia la simulación
     */
    private void resetSimulation() {
        stopSimulation();
        SimulationClock.reset();
        controller = null;
        
        // Limpiar areas
        cpuStateArea.setText("CPU IDLE");
        memoryStateArea.clear();
        ioStateArea.setText("No hay operaciones activas");
        readyQueueArea.setText("Vacía");
        blockedQueueArea.setText("Ninguno");
        ganttArea.clear();
        metricsArea.clear();
        
        timeLabel.setText("Tiempo: 0");
        cpuUtilLabel.setText("Utilización CPU: 0%");
        pageFaultsLabel.setText("Fallos de Pagina: 0");
        
        appendLog("Sistema reiniciado.");
    }
    
    /**
     * Inicializa la cuadrícula visual de memoria
     */
    private void initializeMemoryGrid(int totalFrames) {
        memoryGrid.getChildren().clear();
        memoryFrameLabels = new Label[totalFrames];
        
        int cols = (int) Math.ceil(Math.sqrt(totalFrames));
        
        for (int i = 0; i < totalFrames; i++) {
            Label frameLabel = new Label(String.format("F%d\nLibre", i));
            frameLabel.setPrefSize(80, 60);
            frameLabel.setAlignment(Pos.CENTER);
            frameLabel.setStyle("-fx-border-color: black; -fx-background-color: lightgray;");
            
            memoryFrameLabels[i] = frameLabel;
            memoryGrid.add(frameLabel, i % cols, i / cols);
        }
    }
    
    /**
     * Carga procesos desde archivo
     */
    private void loadProcessesFromFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Cargar Procesos");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Archivos de texto", "*.txt"));
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                List<Process> loaded = ProcessConfigParser.parseFromFile(file.getAbsolutePath());
                processList.addAll(loaded);
                updateProcessList();
                appendLog("Cargados " + loaded.size() + " procesos desde " + file.getName());
            } catch (IOException ex) {
                showAlert("Error", "No se pudo cargar el archivo: " + ex.getMessage());
            }
        }
    }
    
    /**
     * Muestra dialogo para agregar proceso manualmente
     */
    private void showAddProcessDialog() {
        Dialog<Process> dialog = new Dialog<>();
        dialog.setTitle("Agregar Proceso");
        dialog.setHeaderText("Ingrese los datos del proceso");
        
        // Campos del formulario
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField pidField = new TextField();
        pidField.setPromptText("P1");
        Spinner<Integer> arrivalSpinner = new Spinner<>(0, 100, 0);
        TextField burstsField = new TextField();
        burstsField.setPromptText("CPU(5),E/S(2),CPU(3)");
        Spinner<Integer> prioritySpinner = new Spinner<>(1, 10, 1);
        Spinner<Integer> pagesSpinner = new Spinner<>(1, 20, 4);
        
        grid.add(new Label("PID:"), 0, 0);
        grid.add(pidField, 1, 0);
        grid.add(new Label("Tiempo de Llegada:"), 0, 1);
        grid.add(arrivalSpinner, 1, 1);
        grid.add(new Label("Rafagas (formato):"), 0, 2);
        grid.add(burstsField, 1, 2);
        grid.add(new Label("Prioridad:"), 0, 3);
        grid.add(prioritySpinner, 1, 3);
        grid.add(new Label("Paginas:"), 0, 4);
        grid.add(pagesSpinner, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                try {
                    String pid = pidField.getText();
                    int arrival = arrivalSpinner.getValue();
                    List<Burst> bursts = parseBurstsSimple(burstsField.getText());
                    int priority = prioritySpinner.getValue();
                    int pages = pagesSpinner.getValue();
                    
                    return new Process(pid, arrival, bursts, priority, pages);
                } catch (Exception e) {
                    showAlert("Error", "Formato invalido: " + e.getMessage());
                }
            }
            return null;
        });
        
        Optional<Process> result = dialog.showAndWait();
        result.ifPresent(process -> {
            processList.add(process);
            updateProcessList();
            appendLog("Proceso " + process.getPid() + " agregado manualmente.");
        });
    }
    
    /**
     * Parser simple de bursts
     */
    private List<Burst> parseBurstsSimple(String spec) {
        List<Burst> bursts = new ArrayList<>();
        String[] parts = spec.split(",");
        
        for (String part : parts) {
            part = part.trim();
            int open = part.indexOf('(');
            int close = part.indexOf(')');
            
            String type = part.substring(0, open).trim().toUpperCase();
            int duration = Integer.parseInt(part.substring(open + 1, close).trim());
            
            Burst.BurstType burstType = type.equals("CPU") ? Burst.BurstType.CPU : Burst.BurstType.IO;
            bursts.add(new Burst(burstType, duration));
        }
        
        return bursts;
    }
    
    /**
     * Carga procesos de ejemplo
     */
    private void loadExampleProcesses() {
        processList.clear();
        
        processList.add(new Process("P1", 0, Arrays.asList(
            new Burst(Burst.BurstType.CPU, 4),
            new Burst(Burst.BurstType.IO, 3),
            new Burst(Burst.BurstType.CPU, 5)
        ), 1, 4));
        
        processList.add(new Process("P2", 2, Arrays.asList(
            new Burst(Burst.BurstType.CPU, 6),
            new Burst(Burst.BurstType.IO, 2),
            new Burst(Burst.BurstType.CPU, 3)
        ), 2, 5));
        
        processList.add(new Process("P3", 4, Arrays.asList(
            new Burst(Burst.BurstType.CPU, 8)
        ), 3, 6));
        
        processList.add(new Process("P4", 1, Arrays.asList(
            new Burst(Burst.BurstType.CPU, 3),
            new Burst(Burst.BurstType.IO, 2),
            new Burst(Burst.BurstType.CPU, 2),
            new Burst(Burst.BurstType.IO, 1),
            new Burst(Burst.BurstType.CPU, 3)
        ), 1, 3));
        
        updateProcessList();
        appendLog("Cargados 4 procesos de ejemplo.");
    }
    
    /**
     * Limpia la lista de procesos
     */
    private void clearProcesses() {
        processList.clear();
        updateProcessList();
        appendLog("Lista de procesos limpiada.");
    }
    
    /**
     * Actualiza la visualización de la lista de procesos
     */
    private void updateProcessList() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-6s %-8s %-20s %-10s %-8s\n", 
            "PID", "Llegada", "Rafagas", "Prioridad", "Paginas"));
        sb.append("-".repeat(60)).append("\n");
        
        for (Process p : processList) {
            sb.append(String.format("%-6s %-8d %-20s %-10d %-8d\n",
                p.getPid(),
                p.getArrivalTime(),
                formatBursts(p.getBursts()),
                p.getPriority(),
                p.getRequiredPages()));
        }
        
        processListArea.setText(sb.toString());
    }
    
    /**
     * Formatea rafagas para mostrar
     */
    private String formatBursts(List<Burst> bursts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bursts.size(), 3); i++) {
            Burst b = bursts.get(i);
            sb.append(b.getType() == Burst.BurstType.CPU ? "C" : "I")
              .append("(").append(b.getDuration()).append(")");
            if (i < bursts.size() - 1) sb.append(",");
        }
        if (bursts.size() > 3) sb.append("...");
        return sb.toString();
    }
    
    /**
     * Clona procesos para simulación
     */
    private List<Process> cloneProcesses(List<Process> original) {
        List<Process> clones = new ArrayList<>();
        
        for (Process p : original) {
            List<Burst> newBursts = new ArrayList<>();
            for (Burst b : p.getBursts()) {
                newBursts.add(new Burst(b.getType(), b.getDuration()));
            }
            
            Process clone = new Process(
                p.getPid(),
                p.getArrivalTime(),
                newBursts,
                p.getPriority(),
                p.getRequiredPages()
            );
            
            clones.add(clone);
        }
        
        return clones;
    }
    
    /**
     * Crea un scheduler según tipo
     */
    private SchedulingAlgorithm createScheduler(String type, int quantum) {
        switch (type) {
            case "FCFS":
                return new FCFSScheduler();
            case "SJF":
                return new SJFScheduler();
            case "Round Robin":
                return new RoundRobinScheduler(quantum);
            default:
                throw new IllegalArgumentException("Scheduler desconocido: " + type);
        }
    }
    
    /**
     * Crea un algoritmo de reemplazo según tipo
     */
    private PageReplacementAlgorithm createPageAlgorithm(String type) {
        switch (type) {
            case "FIFO":
                return new FIFOPageReplacement();
            case "LRU":
                return new LRUPageReplacement();
            case "Optimal":
                return new OptimalPageReplacement();
            default:
                throw new IllegalArgumentException("Algoritmo desconocido: " + type);
        }
    }
    
    /**
     * Agrega mensaje al log
     */
    private void appendLog(String message) {
        String timestamp = String.format("[%s] ", 
            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        logArea.appendText(timestamp + message + "\n");
    }
    
    /**
     * Muestra alerta
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
