package scheduler.test;

import scheduler.*;
import model.Process;
import model.Burst;
import java.util.Arrays;

/**
 * Prueba unitaria para el algoritmo SJF
 */
public class TestSJF {
  public static void main(String[] args) {
    System.out.println("TEST SJF");

    // Crear scheduler SJF
    SchedulingAlgorithm scheduler = new SJFScheduler();
    ProcessDispatcher dispatcher = new ProcessDispatcher(scheduler);

    // Crear procesos con diferentes tiempos de CPU
    // P1: Llega en t=0, necesita 8 unidades (más largo)
    Process p1 = new Process("P1", 0, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 8)), 1, 3);

    // P2: Llega en t=1, necesita 3 unidades (más corto)
    Process p2 = new Process("P2", 1, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 3)), 1, 3);

    // P3: Llega en t=2, necesita 5 unidades (intermedio)
    Process p3 = new Process("P3", 2, Arrays.asList(
        new Burst(Burst.BurstType.CPU, 5)), 1, 3);

    // Registrar procesos
    dispatcher.registerProcess(p1);
    dispatcher.registerProcess(p2);
    dispatcher.registerProcess(p3);

    // Ejecutar simulación
    dispatcher.runSimulation();

    // Mostrar resultados
    System.out.println(dispatcher.getMetrics());
    
    // Verificar resultados esperados
    System.out.println("\nResultados esperados:");
    
    PerformanceMetrics metrics = dispatcher.getPerformanceMetrics();
    
    // SJF debería ejecutar: P1 primero (llega solo), luego P2 (más corto), luego P3
    // P1: (0-8) Espera=0, Retorno=8, Respuesta=0
    // P2: (8-11) Espera=7, Retorno=10, Respuesta=7
    // P3: (11-16) Espera=9, Retorno=14, Respuesta=9
    // Promedio Espera = (0+7+9)/3 = 5.33
    // Promedio Retorno = (8+10+14)/3 = 10.67
    
    System.out.println("Orden esperado: P1 → P2 → P3");
    System.out.println("(P1 ejecuta primero porque llega solo, luego P2 por ser más corto)");
    System.out.println("Tiempo de espera promedio esperado: 5.33");
    System.out.println("Tiempo de retorno promedio esperado: 10.67");
    
    double avgWait = metrics.getAverageWaitingTime();
    double avgTurnaround = metrics.getAverageTurnaroundTime();
    
    System.out.println("\nResultados obtenidos:");
    System.out.println("Tiempo de espera promedio: " + String.format("%.2f", avgWait));
    System.out.println("Tiempo de retorno promedio: " + String.format("%.2f", avgTurnaround));
    
    // Validación
    if (Math.abs(avgWait - 5.33) < 0.1 && Math.abs(avgTurnaround - 10.67) < 0.1) {
      System.out.println("\nPASO SJF");
    } else {
      System.out.println("\nNECSITA FIX");
    }
  }
}