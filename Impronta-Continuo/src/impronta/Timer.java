package impronta;

public class Timer
{
	// Parámetros
	private static double _tiempoGeneracion = 0;
	private static double _tiempoSolver = 0;
	
	// Momento de inicio de la generación
	private static long _inicio = 0;
	
	// Setters
	public static void setTiempoGeneracion(double nuevo)
	{
		_tiempoGeneracion = nuevo;
	}
	public static void setTiempoSolver(double nuevo)
	{
		_tiempoSolver = nuevo;
	}
	
	// Getters
	public static double getTiempoGeneracion()
	{
		return _tiempoGeneracion;
	}
	public static double getTiempoSolver()
	{
		return _tiempoSolver;
	}

	// Comienza la generación
	public static void comenzar()
	{
		_inicio = System.currentTimeMillis();
	}
	
	// Chequea si se pasó del tiempo de la generación
	public static void chequear()
	{
		if( _tiempoGeneracion != 0 && (System.currentTimeMillis() - _inicio) / 1000.0 > _tiempoGeneracion )
		{
			System.out.println();
			System.out.println("**** Tiempo excedido!");
			System.out.println("**** Generacion del modelo: " + (int)((System.currentTimeMillis() - _inicio) / 1000.0) + " sg.");
			System.out.println("**** Abortando ejecucion");
			
			System.exit(1);
		}
	}
}
