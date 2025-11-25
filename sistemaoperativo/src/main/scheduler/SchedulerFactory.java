package scheduler;

/**
 * Factory para crear instancias de algoritmos de planificacion
 * Centraliza la creacion y permite cambiar facilmente entre algoritmos
 */
public class SchedulerFactory {

  /**
   * Crea un scheduler basado en el tipo especificado
   * 
   * @param type    Tipo de scheduler (FCFS, SJF, RR)
   * @param quantum Quantum para Round Robin (ignorado para otros)
   * @return Instancia del scheduler
   * @throws IllegalArgumentException si el tipo no es soportado
   */
  public static SchedulingAlgorithm createScheduler(String type, int quantum) {
    if (type == null) {
      throw new IllegalArgumentException("Tipo de scheduler no puede ser null");
    }

    switch (type.toUpperCase()) {
      case "FCFS":
        System.out.println("Creando scheduler FCFS");
        return new FCFSScheduler();

      case "SJF":
        System.out.println("Creando scheduler SJF");
        return new SJFScheduler();

      case "RR":
        if (quantum <= 0) {
          throw new IllegalArgumentException("Quantum debe ser mayor a 0 para Round Robin");
        }
        System.out.println("Creando scheduler Round Robin con quantum: " + quantum);
        return new RoundRobinScheduler(quantum);

      default:
        throw new IllegalArgumentException("Tipo de scheduler no soportado: " + type);
    }
  }

  /**
   * Crea scheduler con quantum por defecto para RR
   */
  public static SchedulingAlgorithm createScheduler(String type) {
    return createScheduler(type, 4); // Quantum por defecto: 4
  }
}