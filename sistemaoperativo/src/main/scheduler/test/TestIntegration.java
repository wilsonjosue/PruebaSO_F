package scheduler.test;

import scheduler.*;
import model.Process;
import model.Burst;
import java.util.Arrays;
import java.util.List;

/**
 * Prueba de integración completa para todos los algoritmos
 */
public class TestIntegration {
  public static void main(String[] args) {
    System.out.println("INTEGRATION TEST");
    // Crear conjunto de procesos de prueba para todos
    List<Process> testProcesses = Arrays.asList(
        new Process("P1", 0, Arrays.asList(
            new Burst(Burst.BurstType.CPU, 4),
            new Burst(Burst.BurstType.IO, 3),
            new Burst(Burst.BurstType.CPU, 5)), 1, 4),

        new Process("P2", 2, Arrays.asList(
            new Burst(Burst.BurstType.CPU, 6),
            new Burst(Burst.BurstType.IO, 2),
            new Burst(Burst.BurstType.CPU, 3)), 2, 5),

        new Process("P3", 4, Arrays.asList(
            new Burst(Burst.BurstType.CPU, 8)), 3, 6)
    );

    // Probar los tres algoritmos
    System.out.println("\n" + "═".repeat(60));
    testAlgorithm("FCFS", testProcesses, new FCFSScheduler());
    
    System.out.println("\n\n" + "═".repeat(60));
    testAlgorithm("SJF", testProcesses, new SJFScheduler());
    
    System.out.println("\n\n" + "═".repeat(60));
    testAlgorithm("Round Robin (Quantum=3)", testProcesses, new RoundRobinScheduler(3));
    
    System.out.println("\nCOMPLETADOS TODOS");
  }

  private static void testAlgorithm(String algorithmName, List<Process> originalProcesses,
      SchedulingAlgorithm scheduler) {
    
    System.out.println("ALGORITMO: " + algorithmName);
    System.out.println("═".repeat(60));

    // Crear dispatcher
    ProcessDispatcher dispatcher = new ProcessDispatcher(scheduler);

    // Crear copias de procesos para cada prueba (resetear estado)
    for (Process original : originalProcesses) {
      Process copy = new Process(
          original.getPid(),
          original.getArrivalTime(),
          copyBursts(original.getBursts()),
          original.getPriority(),
          original.getRequiredPages());
      dispatcher.registerProcess(copy);
    }

    // Ejecutar simulación
    dispatcher.runSimulation();

    // Mostrar métricas
    System.out.println(dispatcher.getMetrics());
  }

  private static List<Burst> copyBursts(List<Burst> bursts) {
    return bursts.stream()
        .map(b -> new Burst(b.getType(), b.getDuration()))
        .collect(java.util.stream.Collectors.toList());
  }
}