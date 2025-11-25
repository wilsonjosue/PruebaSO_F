package main;

import model.Process;
import model.Burst;
import scheduler.*;
import memory.*;
import io.IOManager;
import simulation.SimulationController;
import config.ProcessConfigParser;
import java.util.*;
import java.io.IOException;

/**
 * Clase principal del simulador de sistema operativo
 * Permite ejecutar simulaciones con diferentes configuraciones
 */
public class OSSimulator {
  
  public static void main(String[] args) {
    System.out.println("SIMULADOR EDUCATIVO DE SISTEMA OPERATIVO");
    System.out.println("Gestion de Procesos y Memoria Virtual");
    
    // Ejecutar simulacion por defecto
    runDefaultSimulation();
    
    // Ejecutar simulaciones comparativas
    // runComparativeSimulation();
  }
  
  /**
   * Ejecuta una simulacion con configuracion por defecto
   */
  public static void runDefaultSimulation() {
    System.out.println("\n=== SIMULACIoN POR DEFECTO ===\n");
    
    // Crear procesos de ejemplo
    List<Process> processes = createExampleProcesses();
    
    // Configurar algoritmos
    SchedulingAlgorithm scheduler = new RoundRobinScheduler(3);
    PageReplacementAlgorithm pageAlgorithm = new LRUPageReplacement();
    MemoryManager memoryManager = new MemoryManager(10, pageAlgorithm);
    IOManager ioManager = new IOManager();
    
    // Crear y ejecutar simulacion
    SimulationController controller = new SimulationController(
        scheduler, memoryManager, ioManager, 3, 100);
    
    controller.addProcesses(processes);
    controller.runSimulation();
  }
  
  /**
   * Ejecuta simulaciones comparativas con diferentes algoritmos
   */
  public static void runComparativeSimulation() {
    System.out.println("\n=== SIMULACIONES COMPARATIVAS ===\n");
    
    List<Process> baseProcesses = createExampleProcesses();
    
    // Probar diferentes combinaciones
    String[] schedulerNames = {"FCFS", "SJF", "Round Robin"};
    String[] memoryAlgorithms = {"FIFO", "LRU", "Optimal"};
    
    for (String schedName : schedulerNames) {
      for (String memAlg : memoryAlgorithms) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println(String.format("Simulacion: %s + %s", schedName, memAlg));
        System.out.println("=".repeat(70));
        
        // Crear copias de procesos (resetear estado)
        List<Process> processes = cloneProcesses(baseProcesses);
        
        // Crear scheduler
        SchedulingAlgorithm scheduler = createScheduler(schedName, 3);
        
        // Crear algoritmo de memoria
        PageReplacementAlgorithm pageAlgorithm = createPageAlgorithm(memAlg);
        
        // Crear managers
        MemoryManager memoryManager = new MemoryManager(10, pageAlgorithm);
        IOManager ioManager = new IOManager();
        
        // Ejecutar simulacion
        SimulationController controller = new SimulationController(
            scheduler, memoryManager, ioManager, 3, 100);
        
        controller.addProcesses(processes);
        controller.runSimulation();
        
        System.out.println("\nPresione Enter para continuar...");
        try {
          System.in.read();
        } catch (IOException e) {
          // Ignorar
        }
      }
    }
  }
  
  /**
   * Crea procesos de ejemplo para la simulacion
   */
  private static List<Process> createExampleProcesses() {
    List<Process> processes = new ArrayList<>();
    
    // P1: Proceso con CPU y E/S
    List<Burst> bursts1 = Arrays.asList(
        new Burst(Burst.BurstType.CPU, 4),
        new Burst(Burst.BurstType.IO, 3),
        new Burst(Burst.BurstType.CPU, 5)
    );
    processes.add(new Process("P1", 0, bursts1, 1, 4));
    
    // P2: Proceso con CPU, E/S y mas CPU
    List<Burst> bursts2 = Arrays.asList(
        new Burst(Burst.BurstType.CPU, 6),
        new Burst(Burst.BurstType.IO, 2),
        new Burst(Burst.BurstType.CPU, 3)
    );
    processes.add(new Process("P2", 2, bursts2, 2, 5));
    
    // P3: Proceso solo CPU
    List<Burst> bursts3 = Arrays.asList(
        new Burst(Burst.BurstType.CPU, 8)
    );
    processes.add(new Process("P3", 4, bursts3, 3, 6));
    
    // P4: Proceso con múltiples E/S
    List<Burst> bursts4 = Arrays.asList(
        new Burst(Burst.BurstType.CPU, 3),
        new Burst(Burst.BurstType.IO, 2),
        new Burst(Burst.BurstType.CPU, 2),
        new Burst(Burst.BurstType.IO, 1),
        new Burst(Burst.BurstType.CPU, 3)
    );
    processes.add(new Process("P4", 1, bursts4, 1, 3));
    
    return processes;
  }
  
  /**
   * Clona una lista de procesos (resetea estado)
   */
  private static List<Process> cloneProcesses(List<Process> original) {
    List<Process> clones = new ArrayList<>();
    
    for (Process p : original) {
      // Crear nueva lista de bursts
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
   * Crea un scheduler según el nombre
   */
  private static SchedulingAlgorithm createScheduler(String name, int quantum) {
    switch (name) {
      case "FCFS":
        return new FCFSScheduler();
      case "SJF":
        return new SJFScheduler();
      case "Round Robin":
        return new RoundRobinScheduler(quantum);
      default:
        throw new IllegalArgumentException("Scheduler desconocido: " + name);
    }
  }
  
  /**
   * Crea un algoritmo de reemplazo de paginas según el nombre
   */
  private static PageReplacementAlgorithm createPageAlgorithm(String name) {
    switch (name) {
      case "FIFO":
        return new FIFOPageReplacement();
      case "LRU":
        return new LRUPageReplacement();
      case "Optimal":
        return new OptimalPageReplacement();
      default:
        throw new IllegalArgumentException("Algoritmo de memoria desconocido: " + name);
    }
  }
  
  /**
   * Ejecuta una simulacion desde archivo de configuracion
   */
  public static void runFromFile(String configFile) {
    try {
      System.out.println(String.format("\n=== SIMULACIoN DESDE ARCHIVO: %s ===\n", configFile));
      
      // Parsear procesos desde archivo
      List<Process> processes = ProcessConfigParser.parseFromFile(configFile);
      
      if (processes.isEmpty()) {
        System.err.println("No se encontraron procesos en el archivo");
        return;
      }
      
      // Configurar algoritmos (valores por defecto)
      SchedulingAlgorithm scheduler = new RoundRobinScheduler(3);
      PageReplacementAlgorithm pageAlgorithm = new LRUPageReplacement();
      MemoryManager memoryManager = new MemoryManager(10, pageAlgorithm);
      IOManager ioManager = new IOManager();
      
      // Crear y ejecutar simulacion
      SimulationController controller = new SimulationController(
          scheduler, memoryManager, ioManager, 3, 150);
      
      controller.addProcesses(processes);
      controller.runSimulation();
      
    } catch (IOException e) {
      System.err.println("Error leyendo archivo: " + e.getMessage());
    }
  }
  
  /**
   * Crea un archivo de ejemplo de configuracion
   */
  public static void createExampleConfigFile(String filePath) {
    try {
      List<Process> processes = createExampleProcesses();
      ProcessConfigParser.saveToFile(processes, filePath);
      System.out.println("Archivo de ejemplo creado: " + filePath);
    } catch (IOException e) {
      System.err.println("Error creando archivo: " + e.getMessage());
    }
  }
  
  /**
   * Imprime ayuda de uso
   */
  public static void printUsage() {
    System.out.println("\nUSO DEL SIMULADOR:");
    System.out.println("  java main.OSSimulator [opciones]");
    System.out.println("\nOPCIONES:");
    System.out.println("  --file <archivo>     Ejecutar simulacion desde archivo");
    System.out.println("  --create <archivo>   Crear archivo de ejemplo");
    System.out.println("  --compare            Ejecutar simulaciones comparativas");
    System.out.println("  --help               Mostrar esta ayuda");
    System.out.println("\nEJEMPLOS:");
    System.out.println("  java main.OSSimulator");
    System.out.println("  java main.OSSimulator --file procesos.txt");
    System.out.println("  java main.OSSimulator --create ejemplo.txt");
    System.out.println("  java main.OSSimulator --compare");
  }
}
