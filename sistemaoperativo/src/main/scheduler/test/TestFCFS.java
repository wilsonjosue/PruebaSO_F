package scheduler.test;

import scheduler.*;
import model.Process;
import model.Burst;
import java.util.Arrays;

/**
 * Prueba unitaria para el algoritmo FCFS
 */
public class TestFCFS {
  public static void main(String[] args) {
    System.out.println("Test FCFS");

    // Crear scheduler FCFS
    SchedulingAlgorithm scheduler = new FCFSScheduler();
    ProcessDispatcher dispatcher = new ProcessDispatcher(scheduler);

    // Crear procesos de prueba
    // P1: Llega en t=0, necesita 5 unidades de CPU
    Process p1 = new Process("P1", 0, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 5)), 1, 3);

    // P2: Llega en t=1, necesita 3 unidades de CPU
    Process p2 = new Process("P2", 1, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 3)), 1, 3);

    // P3: Llega en t=2, necesita 4 unidades de CPU
    Process p3 = new Process("P3", 2, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 4)), 1, 3);

    // Registrar procesos
    dispatcher.registerProcess(p1);
    dispatcher.registerProcess(p2);
    dispatcher.registerProcess(p3);

    // Ejecutar simulación
    dispatcher.runSimulation();

    // Mostrar resultados
    System.out.println(dispatcher.getMetrics());
    
    // Verificar resultados esperados
    System.out.println("\nVerificación de resultados");
    
    PerformanceMetrics metrics = dispatcher.getPerformanceMetrics();
    
    // FCFS debería ejecutar en orden: P1 (0-5), P2 (5-8), P3 (8-12)
    // P1: Espera=0, Retorno=5, Respuesta=0
    // P2: Espera=4, Retorno=7, Respuesta=4
    // P3: Espera=6, Retorno=10, Respuesta=6
    // Promedio Espera = (0+4+6)/3 = 3.33
    // Promedio Retorno = (5+7+10)/3 = 7.33
    
    System.out.println("Orden esperado: P1 → P2 → P3");
    System.out.println("Tiempo de espera promedio que deberia ser: 3.33");
    System.out.println("Tiempo de retorno promedio que deberia ser: 7.33");

    double avgWait = metrics.getAverageWaitingTime();
    double avgTurnaround = metrics.getAverageTurnaroundTime();
    
    System.out.println("\nResultados:");
    System.out.println("Tiempo de espera promedio: " + String.format("%.2f", avgWait));
    System.out.println("Tiempo de retorno promedio: " + String.format("%.2f", avgTurnaround));
    
    // Validación simple
    if (Math.abs(avgWait - 3.33) < 0.1 && Math.abs(avgTurnaround - 7.33) < 0.1) {
      System.out.println("\nTEST FCFS COMPLETADO");
    } else {
      System.out.println("\nTEST FCFS FALLO, las Métricas no coinciden");
    }
  }
}