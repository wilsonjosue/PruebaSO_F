package scheduler.test;

import model.Process;
import model.Burst;
import scheduler.*;
import memory.*;
import io.IOManager;
import simulation.SimulationController;
import config.ProcessConfigParser;
import java.util.*;

/**
 * Tests integrales del simulador completo
 */
public class TestCompleteSimulation {
  
  public static void main(String[] args) {
    System.out.println("=== TESTS DE SIMULACIÓN COMPLETA ===\n");
    
    testBasicSimulation();
    testMemoryManagement();
    testIOHandling();
    testDifferentSchedulers();
    testPageReplacementAlgorithms();
    testFileParser();
    
    System.out.println("\n=== TODOS LOS TESTS COMPLETADOS ===");
  }
  
  /**
   * Test basico de simulación con Round Robin
   */
  public static void testBasicSimulation() {
    System.out.println("\n--- TEST 1: Simulación Basica con Round Robin ---");
    
    List<Process> processes = new ArrayList<>();
    processes.add(createSimpleProcess("P1", 0, 10, 3));
    processes.add(createSimpleProcess("P2", 2, 8, 3));
    processes.add(createSimpleProcess("P3", 4, 6, 3));
    
    SchedulingAlgorithm scheduler = new RoundRobinScheduler(3);
    MemoryManager memory = new MemoryManager(10, new LRUPageReplacement());
    IOManager ioManager = new IOManager();
    
    SimulationController controller = new SimulationController(
        scheduler, memory, ioManager, 3, 100);
    
    controller.addProcesses(processes);
    controller.runSimulation();
    
    System.out.println("\nTest basico completado");
  }
  
  /**
   * Test de gestión de memoria con diferentes algoritmos
   */
  public static void testMemoryManagement() {
    System.out.println("\n--- TEST 2: Gestión de Memoria ---");
    
    List<Process> processes = new ArrayList<>();
    // Crear procesos que requieren mas paginas que marcos disponibles
    processes.add(createSimpleProcess("P1", 0, 5, 4));
    processes.add(createSimpleProcess("P2", 1, 6, 5));
    processes.add(createSimpleProcess("P3", 2, 4, 3));
    
    // Test con memoria limitada (solo 8 marcos)
    SchedulingAlgorithm scheduler = new FCFSScheduler();
    MemoryManager memory = new MemoryManager(8, new FIFOPageReplacement());
    IOManager ioManager = new IOManager();
    
    SimulationController controller = new SimulationController(
        scheduler, memory, ioManager, 0, 100);
    
    controller.addProcesses(processes);
    controller.runSimulation();
    
    // Verificar que hubo fallos de pagina
    int pageFaults = memory.getPageFaults();
    System.out.println("\nFallos de pagina: " + pageFaults);
    assert pageFaults > 0 : "Debería haber fallos de pagina";
    
    System.out.println("\nTest de memoria completado");
  }
  
  /**
   * Test de manejo de E/S
   */
  public static void testIOHandling() {
    System.out.println("\n--- TEST 3: Manejo de E/S ---");
    
    List<Process> processes = new ArrayList<>();
    
    // Crear procesos con operaciones de E/S
    List<Burst> bursts1 = Arrays.asList(
        new Burst(Burst.BurstType.CPU, 3),
        new Burst(Burst.BurstType.IO, 5),
        new Burst(Burst.BurstType.CPU, 2)
    );
    processes.add(new Process("P1", 0, bursts1, 1, 3));
    
    List<Burst> bursts2 = Arrays.asList(
        new Burst(Burst.BurstType.CPU, 4),
        new Burst(Burst.BurstType.IO, 3),
        new Burst(Burst.BurstType.CPU, 3)
    );
    processes.add(new Process("P2", 1, bursts2, 1, 3));
    
    SchedulingAlgorithm scheduler = new FCFSScheduler();
    MemoryManager memory = new MemoryManager(10, new LRUPageReplacement());
    IOManager ioManager = new IOManager();
    
    SimulationController controller = new SimulationController(
        scheduler, memory, ioManager, 0, 100);
    
    controller.addProcesses(processes);
    controller.runSimulation();
    
    // Verificar que hubo operaciones de E/S
    int ioOps = ioManager.getCompletedIOOperations();
    System.out.println("\nOperaciones de E/S completadas: " + ioOps);
    assert ioOps > 0 : "Debería haber operaciones de E/S";
    
    System.out.println("\nTest de E/S completado");
  }
  
  /**
   * Test con diferentes algoritmos de planificación
   */
  public static void testDifferentSchedulers() {
    System.out.println("\n--- TEST 4: Diferentes Algoritmos de Planificación ---");
    
    List<Process> baseProcesses = new ArrayList<>();
    baseProcesses.add(createSimpleProcess("P1", 0, 8, 3));
    baseProcesses.add(createSimpleProcess("P2", 1, 4, 3));
    baseProcesses.add(createSimpleProcess("P3", 2, 9, 3));
    baseProcesses.add(createSimpleProcess("P4", 3, 5, 3));
    
    String[] schedulerNames = {"FCFS", "SJF", "Round Robin"};
    
    for (String name : schedulerNames) {
      System.out.println("\n>> Probando " + name);
      
      // Clonar procesos
      List<Process> processes = cloneProcesses(baseProcesses);
      
      SchedulingAlgorithm scheduler;
      int quantum = 0;
      
      switch (name) {
        case "FCFS":
          scheduler = new FCFSScheduler();
          break;
        case "SJF":
          scheduler = new SJFScheduler();
          break;
        case "Round Robin":
          scheduler = new RoundRobinScheduler(3);
          quantum = 3;
          break;
        default:
          continue;
      }
      
      MemoryManager memory = new MemoryManager(12, new LRUPageReplacement());
      IOManager ioManager = new IOManager();
      
      SimulationController controller = new SimulationController(
          scheduler, memory, ioManager, quantum, 100);
      
      controller.addProcesses(processes);
      controller.runSimulation();
      
      // Verificar métricas
      PerformanceMetrics metrics = scheduler.getPerformanceMetrics();
      System.out.println(String.format("Tiempo promedio de espera: %.2f",
          metrics.getAverageWaitingTime()));
      System.out.println(String.format("Tiempo promedio de retorno: %.2f",
          metrics.getAverageTurnaroundTime()));
    }
    
    System.out.println("\nTest de algoritmos completado");
  }
  
  /**
   * Test con diferentes algoritmos de reemplazo de paginas
   */
  public static void testPageReplacementAlgorithms() {
    System.out.println("\n--- TEST 5: Algoritmos de Reemplazo de Paginas ---");
    
    List<Process> baseProcesses = new ArrayList<>();
    baseProcesses.add(createSimpleProcess("P1", 0, 5, 5));
    baseProcesses.add(createSimpleProcess("P2", 1, 6, 4));
    baseProcesses.add(createSimpleProcess("P3", 2, 4, 6));
    
    String[] algorithms = {"FIFO", "LRU"};
    
    for (String alg : algorithms) {
      System.out.println("\n>> Probando " + alg);
      
      List<Process> processes = cloneProcesses(baseProcesses);
      
      PageReplacementAlgorithm pageAlgorithm;
      switch (alg) {
        case "FIFO":
          pageAlgorithm = new FIFOPageReplacement();
          break;
        case "LRU":
          pageAlgorithm = new LRUPageReplacement();
          break;
        default:
          continue;
      }
      
      SchedulingAlgorithm scheduler = new FCFSScheduler();
      MemoryManager memory = new MemoryManager(8, pageAlgorithm); // Memoria limitada
      IOManager ioManager = new IOManager();
      
      SimulationController controller = new SimulationController(
          scheduler, memory, ioManager, 0, 100);
      
      controller.addProcesses(processes);
      controller.runSimulation();
      
      System.out.println(String.format("Fallos de pagina: %d", memory.getPageFaults()));
      System.out.println(String.format("Reemplazos: %d", memory.getPageReplacements()));
    }
    
    System.out.println("\nTest de reemplazo de paginas completado");
  }
  
  /**
   * Test del parser de archivos
   */
  public static void testFileParser() {
    System.out.println("\n--- TEST 6: Parser de Archivos ---");
    
    try {
      // Intentar leer archivo de configuración
      String configFile = "config/procesos.txt";
      List<Process> processes = ProcessConfigParser.parseFromFile(configFile);
      
      System.out.println(String.format("Procesos parseados: %d", processes.size()));
      
      for (Process p : processes) {
        System.out.println(String.format("  %s: Llegada=%d, Rafagas=%d, Paginas=%d",
            p.getPid(), p.getArrivalTime(), p.getBursts().size(), p.getRequiredPages()));
      }
      
      // Ejecutar simulación con procesos del archivo
      if (!processes.isEmpty()) {
        SchedulingAlgorithm scheduler = new RoundRobinScheduler(3);
        MemoryManager memory = new MemoryManager(15, new LRUPageReplacement());
        IOManager ioManager = new IOManager();
        
        SimulationController controller = new SimulationController(
            scheduler, memory, ioManager, 3, 150);
        
        controller.addProcesses(processes);
        controller.runSimulation();
      }
      
      System.out.println("\nTest de parser completado");
      
    } catch (Exception e) {
      System.err.println("Error en test de parser: " + e.getMessage());
      System.out.println("(Nota: Es normal si el archivo no existe)");
    }
  }
  
  // Métodos auxiliares
  
  private static Process createSimpleProcess(String pid, int arrival, int cpuTime, int pages) {
    List<Burst> bursts = Arrays.asList(new Burst(Burst.BurstType.CPU, cpuTime));
    return new Process(pid, arrival, bursts, 1, pages);
  }
  
  private static List<Process> cloneProcesses(List<Process> original) {
    List<Process> clones = new ArrayList<>();
    for (Process p : original) {
      List<Burst> newBursts = new ArrayList<>();
      for (Burst b : p.getBursts()) {
        newBursts.add(new Burst(b.getType(), b.getDuration()));
      }
      Process clone = new Process(p.getPid(), p.getArrivalTime(), newBursts,
          p.getPriority(), p.getRequiredPages());
      clones.add(clone);
    }
    return clones;
  }
}
