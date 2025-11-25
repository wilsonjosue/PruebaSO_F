package config;

import model.Process;
import model.Burst;
import java.io.*;
import java.util.*;

/**
 * Parser para leer configuracion de procesos desde archivo
 * Formato: PID ArrivalTime Bursts Priority Pages
 * Ejemplo: P1 0 CPU(4),E/S(3),CPU(5) 1 4
 */
public class ProcessConfigParser {
  
  /**
   * Lee procesos desde un archivo
   * 
   * @param filePath Ruta del archivo
   * @return Lista de procesos parseados
   * @throws IOException Si hay error leyendo el archivo
   */
  public static List<Process> parseFromFile(String filePath) throws IOException {
    List<Process> processes = new ArrayList<>();
    
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
      String line;
      int lineNumber = 0;
      
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        line = line.trim();
        
        // Ignorar líneas vacías y comentarios
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
          continue;
        }
        
        try {
          Process process = parseLine(line);
          processes.add(process);
        } catch (Exception e) {
          System.err.println(String.format("Error en línea %d: %s - %s",
              lineNumber, line, e.getMessage()));
        }
      }
    }
    
    System.out.println(String.format("Parseados %d procesos desde %s", processes.size(), filePath));
    return processes;
  }
  
  /**
   * Parsea una línea de configuracion
   * 
   * @param line Línea a parsear
   * @return Proceso creado
   */
  private static Process parseLine(String line) {
    String[] parts = line.split("\\s+");
    
    if (parts.length < 5) {
      throw new IllegalArgumentException("Formato invalido. Esperado: PID ArrivalTime Bursts Priority Pages");
    }
    
    String pid = parts[0];
    int arrivalTime = Integer.parseInt(parts[1]);
    List<Burst> bursts = parseBursts(parts[2]);
    int priority = Integer.parseInt(parts[3]);
    int requiredPages = Integer.parseInt(parts[4]);
    
    return new Process(pid, arrivalTime, bursts, priority, requiredPages);
  }
  
  /**
   * Parsea la especificacion de rafagas
   * Formato: CPU(4),E/S(3),CPU(5) o CPU(4),IO(3),CPU(5)
   * 
   * @param burstSpec Especificacion de rafagas
   * @return Lista de rafagas
   */
  private static List<Burst> parseBursts(String burstSpec) {
    List<Burst> bursts = new ArrayList<>();
    String[] burstStrings = burstSpec.split(",");
    
    for (String burstStr : burstStrings) {
      burstStr = burstStr.trim();
      
      // Formato: TYPE(duration)
      int openParen = burstStr.indexOf('(');
      int closeParen = burstStr.indexOf(')');
      
      if (openParen == -1 || closeParen == -1) {
        throw new IllegalArgumentException("Formato de rafaga invalido: " + burstStr);
      }
      
      String type = burstStr.substring(0, openParen).trim().toUpperCase();
      int duration = Integer.parseInt(burstStr.substring(openParen + 1, closeParen).trim());
      
      Burst.BurstType burstType;
      if (type.equals("CPU")) {
        burstType = Burst.BurstType.CPU;
      } else if (type.equals("E/S") || type.equals("IO") || type.equals("I/O")) {
        burstType = Burst.BurstType.IO;
      } else {
        throw new IllegalArgumentException("Tipo de rafaga desconocido: " + type);
      }
      
      bursts.add(new Burst(burstType, duration));
    }
    
    return bursts;
  }
  
  /**
   * Crea procesos desde una configuracion en memoria
   * 
   * @param configs Lista de configuraciones de procesos
   * @return Lista de procesos
   */
  public static List<Process> createProcesses(List<ProcessConfig> configs) {
    List<Process> processes = new ArrayList<>();
    
    for (ProcessConfig config : configs) {
      Process process = new Process(
          config.pid,
          config.arrivalTime,
          config.bursts,
          config.priority,
          config.requiredPages
      );
      processes.add(process);
    }
    
    return processes;
  }
  
  /**
   * Guarda procesos a un archivo
   * 
   * @param processes Lista de procesos
   * @param filePath Ruta del archivo
   * @throws IOException Si hay error escribiendo el archivo
   */
  public static void saveToFile(List<Process> processes, String filePath) throws IOException {
    try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
      writer.println("# Configuracion de procesos");
      writer.println("# Formato: PID ArrivalTime Bursts Priority Pages");
      writer.println("# Ejemplo: P1 0 CPU(4),E/S(3),CPU(5) 1 4");
      writer.println();
      
      for (Process p : processes) {
        String burstSpec = formatBursts(p.getBursts());
        writer.println(String.format("%s %d %s %d %d",
            p.getPid(),
            p.getArrivalTime(),
            burstSpec,
            p.getPriority(),
            p.getRequiredPages()));
      }
    }
    
    System.out.println(String.format("Guardados %d procesos en %s", processes.size(), filePath));
  }
  
  /**
   * Formatea rafagas para guardar
   */
  private static String formatBursts(List<Burst> bursts) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bursts.size(); i++) {
      Burst burst = bursts.get(i);
      String type = burst.getType() == Burst.BurstType.CPU ? "CPU" : "E/S";
      sb.append(String.format("%s(%d)", type, burst.getDuration()));
      if (i < bursts.size() - 1) {
        sb.append(",");
      }
    }
    return sb.toString();
  }
  
  /**
   * Clase auxiliar para configuracion de procesos
   */
  public static class ProcessConfig {
    public String pid;
    public int arrivalTime;
    public List<Burst> bursts;
    public int priority;
    public int requiredPages;
    
    public ProcessConfig(String pid, int arrivalTime, List<Burst> bursts, 
                        int priority, int requiredPages) {
      this.pid = pid;
      this.arrivalTime = arrivalTime;
      this.bursts = bursts;
      this.priority = priority;
      this.requiredPages = requiredPages;
    }
  }
}
