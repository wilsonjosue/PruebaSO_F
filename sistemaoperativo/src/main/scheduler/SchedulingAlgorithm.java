//src/main/scheduler/SchedulingAlgorithm.java
package scheduler;

import model.Process;
import java.util.List;

/**
 * Interface común para todos los algoritmos de planificacion.
 * Define los métodos que deben implementar todos los schedulers.
 */
public interface SchedulingAlgorithm {

  //Obtiene el proximo proceso a ejecutar según el algoritmo
  //@return Proceso a ejecutar, o null si no hay procesos listos
  Process getNextProcess();

  //Agrega un proceso a la cola de listos
  //@param process Proceso a agregar
  void addProcess(Process process);

  /**
   * Se llama cuando un proceso completa su rafaga de CPU actual
   * @param process Proceso que completo su ejecucion
   */
  void onProcessCompletion(Process process);

  /**
   * Se llama cuando un proceso es interrumpido (solo para algoritmos apropiativos).
   * CONTRATO IMPORTANTE:
   * Si {@link #isPreemptive()} devuelve true, la implementación de
   * onProcessInterrupted(...) debe **reinsertar** el proceso en la cola de listos
   * (por ejemplo, llamando a addProcess(process)). Si isPreemptive() es false,
   * la implementación puede optar por no reinsertar el proceso.
   * @param process Proceso interrumpido
   */
  void onProcessInterrupted(Process process);
  
  //Se llama cuando un proceso comienza su primera ejecucion
  //@param process Proceso que inicia
  void onProcessStarted(Process process);
  
  /**
   * Registra tiempo de CPU ejecutado
   * @param process Proceso que ejecuto
   * @param time Tiempo ejecutado
   */
  void recordCPUExecution(Process process, int time);

  /**
   * Obtiene la cola actual de procesos listos para visualizacion
   * @return Lista de procesos en cola de listos
   */
  List<Process> getReadyQueue();

  /**
   * Verifica si el algoritmo es apropiativo
   * @return true si es apropiativo (puede interrumpir), false si no
   */
  boolean isPreemptive();

  /**
   * Obtiene métricas del scheduler
   * @return String con métricas de rendimiento
   */
  String getMetrics();
  
  /**
   * Obtiene el objeto de métricas de rendimiento
   * @return Objeto PerformanceMetrics
   */
  PerformanceMetrics getPerformanceMetrics();
}