package scheduler.test;

import scheduler.*;
import model.Process;
import model.Burst;
import java.util.Arrays;

/**
 * Prueba unitaria para el algoritmo Round Robin
 */
public class TestRoundRobin {
  public static void main(String[] args) {
    System.out.println("TEST ROUND ROBIN");

    // Crear scheduler Round Robin con quantum = 2
    SchedulingAlgorithm scheduler = new RoundRobinScheduler(2);
    ProcessDispatcher dispatcher = new ProcessDispatcher(scheduler);

    // Crear procesos
    // P1: Llega en t=0, necesita 5 unidades de CPU
    Process p1 = new Process("P1", 0, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 5)), 1, 3);

    // P2: Llega en t=1, necesita 3 unidades de CPU
    Process p2 = new Process("P2", 1, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 3)), 1, 3);

    // P3: Llega en t=2, necesita 2 unidades de CPU
    Process p3 = new Process("P3", 2, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 2)), 1, 3);

    // Registrar procesos
    dispatcher.registerProcess(p1);
    dispatcher.registerProcess(p2);
    dispatcher.registerProcess(p3);

    // Ejecutar simulación
    dispatcher.runSimulation();

    // Mostrar resultados
    System.out.println(dispatcher.getMetrics());
    
    // Verificar resultados esperados
    System.out.println("VERIFICACIÓN DE RESULTADOS");
    
    PerformanceMetrics metrics = dispatcher.getPerformanceMetrics();
    
    // Round Robin (quantum=2) debería ejecutar:
    // T0-1: P1 (2 unidades) -> P1 restante=3
    // T2: P1 interrumpido, P2 llega
    // T2-3: P2 (2 unidades) -> P2 restante=1
    // T4: P2 interrumpido, P3 llega
    // T4-5: P3 (2 unidades) -> P3 completado
    // T6-7: P1 (2 unidades) -> P1 restante=1
    // T8: P2 (1 unidad) -> P2 completado
    // T9: P1 (1 unidad) -> P1 completado
    
    System.out.println("Quantum: 2");
    System.out.println("Secuencia esperada: P1→P2→P3→P1→P2→P1");
    System.out.println("Tiempo de respuesta promedio esperado: bajo (todos responden rapido)");
    
    double avgWait = metrics.getAverageWaitingTime();
    double avgTurnaround = metrics.getAverageTurnaroundTime();
    double avgResponse = metrics.getAverageResponseTime();
    
    System.out.println("\nResultados obtenidos:");
    System.out.println("Tiempo de espera promedio: " + String.format("%.2f", avgWait));
    System.out.println("Tiempo de retorno promedio: " + String.format("%.2f", avgTurnaround));
    System.out.println("Tiempo de respuesta promedio: " + String.format("%.2f", avgResponse));
    
    RoundRobinScheduler rrScheduler = (RoundRobinScheduler) scheduler;
    System.out.println("Context switches: " + rrScheduler.getContextSwitches());
    
    // Validación (Round Robin debe tener buen tiempo de respuesta)
    if (avgResponse < 5.0) {
      System.out.println("\nPASO ROUND ROBIN");
    } else {
      System.out.println("\nNECESITA FIX");
    }
  }
}