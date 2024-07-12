package impronta;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Scanner;

public class SolverScip extends SolverDiscreto
{
	private Process _scip;

	// Constructor
	public SolverScip(Instancia instancia)
	{
		super(instancia);
	}
	
	// Resuelve la instancia
	public Solucion resolver()
	{
		construirDiscretizacion();
		generarPads();
		construirGrafo();

		escribirModelo("impronta.lp");
		escribirParametros();
		resolverModelo();
		return tomarResultados();
	}

	// Escribe el modelo en formato .lp
	private void escribirModelo(String archivo)
	{
		System.out.println("Generando modelo ...");
		System.out.println();

		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(archivo));
			long inicioObjetivo = System.currentTimeMillis();
			
			// Función objetivo
			StringBuilder fobj = new StringBuilder(1000); // 20 x 50
			fobj.append("Maximize ");
			for(int i=0; i<_pads.size(); ++i)
			{
				fobj.append(i>0 ? " + " : "");
				fobj.append(_grafo.getPeso(i));
				fobj.append(" x");
				fobj.append(i);
				
				if( (i+1) % 20 == 0 )
				{
					fobj.append("\n");
					writer.write(fobj.toString());
					fobj = new StringBuilder(1000);
				}
				
				if( (i+1) % (_pads.size() / 20) == 0 )
				{
					System.out.println("  -> Funcion objetivo " + Math.round((i+1) * 100.0 / _pads.size()) + "% generada" );
					Timer.chequear();
				}
			}
			
			fobj.append("\n");
			writer.write(fobj.toString());

			System.out.println();
			System.out.println("  -> Funcion objetivo generada: " + ((System.currentTimeMillis() - inicioObjetivo) / 1000.0) + " seg." );
			System.out.println();
			
			long inicioRestricciones = System.currentTimeMillis();
			
			// Restricciones
			int k = 0;
			writer.write("Subject to\n");
			for(Grafo.Clique clique: _grafo.getCliques())
			{
				StringBuilder constr = new StringBuilder(1000);
				for(Integer i: clique)
				{
					constr.append(constr.length() == 0 ? "" : " + ");
					constr.append("x");
					constr.append(i);
				}
				
				if( constr.length() > 0 )
				{
					constr.append(" <= 1\n");
					writer.write(constr.toString());
				}
				
				if( (++k) % (_discretizacion.getPuntos().getNumPoints() / 20) == 0 )
				{
					System.out.println("  -> Restricciones " + Math.round(k * 100.0 / _discretizacion.getPuntos().getNumPoints()) + "% generadas" );
					Timer.chequear();
				}
			}

			// Variables
			writer.write("Binary\n");
			for(int i=0; i<_pads.size(); ++i)
				writer.write( "x" + i + "\n");
			
			writer.write("End\n");
			writer.close();

			System.out.println();
			System.out.println("  -> Modelo generado: " + ((System.currentTimeMillis() - inicioRestricciones) / 1000.0) + " seg." );
			System.out.println();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	// Escribe los parámetros de SCIP
	private void escribirParametros()
	{
		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter("scip.set"));
			
			if( Timer.getTiempoSolver() != 0 )
				writer.write("limits/time = " + Timer.getTiempoSolver() + "\n");
			
			writer.write("limits/gap = 0.0\n");
			writer.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		System.out.println("  -> Archivo de parametros scip.set escrito");
		System.out.println();
	}

	// Resuelve el modelo
	private void resolverModelo()
	{
		// Verifica que existan los archivos
		File scip = new File("scip");
		File scip2 = new File("scip.exe");
		File zpl = new File("impronta.lp");
		File sets = new File("scip.set");
		
		if( scip.exists() == false && scip2.exists() == false )
			throw new RuntimeException("No se pudo encontrar el archivo del solver.\nLa reinstalación de la aplicación puede solucionar este problema.");

		if( zpl.exists() == false )
			throw new RuntimeException("No se pudo encontrar el archivo con el modelo impronta.lp.\nLa reinstalación de la aplicación puede solucionar este problema.");
		
		if( sets.exists() == false )
			throw new RuntimeException("No se pudo encontrar el archivo de configuracion de scip.\nLa reinstalación de la aplicación puede solucionar este problema.");

		File salida = new File("salida.txt");
		if( salida.exists() == true )
			salida.delete();
		
		// Ejecuta el solver
		try
		{
		    try
		    {
		    	if( scip.exists() )
		    		_scip = Runtime.getRuntime().exec("./scip -f impronta.lp -s scip.set -l salida.txt");
		    	
		    	if( scip2.exists() )
		    		_scip = Runtime.getRuntime().exec("scip.exe -f impronta.lp -s scip.set -l salida.txt");

		    	BufferedReader bri = new BufferedReader(new InputStreamReader(_scip.getInputStream()));
		        BufferedReader bre = new BufferedReader(new InputStreamReader(_scip.getErrorStream()));
		        
		        String linea;
		        while ((linea = bri.readLine()) != null)
		        	System.out.println(linea);

		        bri.close();

		        while ((linea = bre.readLine()) != null)
		        	System.out.println(linea);

		        bre.close();
		        _scip.waitFor();
		      }
		      catch (Exception err)
		      {
		        err.printStackTrace();
		      }
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Se encontraron problemas al resolver! No es posible mostrar la solución.\n\nError:\n" + ex.getMessage());
		}
	}
	
	// Interrumpe la ejecución
	public void interrumpirSolver()
	{
		_scip.destroy();
	}

	// Obtiene los resultados de la salida de SCIP
	private Solucion tomarResultados()
	{
		System.out.println();
		System.out.println("Tomando solución ...");
		System.out.println();

		// Verifica que exista el archivo de salida
		File salida = new File("salida.txt");
		if( salida.exists() == false )
			throw new RuntimeException("No se pudo encontrar el archivo de resultados salida.txt.\nEs posible que se hayan producido problemas al resolver.");
		
		Solucion ret = new Solucion(_instancia);
		
		try
		{
			Scanner scanner = new Scanner(salida);
			while( scanner.hasNextLine() && !scanner.nextLine().contains("primal solution")) { }
			scanner.nextLine();
			scanner.nextLine();
			scanner.nextLine();
			
			String linea = scanner.nextLine();
			while( linea.trim().length() > 0 )
			{
				if( linea.charAt(0) == 'x')
				{
					String indice = linea.substring(1, linea.indexOf(" "));
					int ni = Integer.parseInt(indice);
					ret.agregarPad( _pads.get(ni) );
				}
				
				linea = scanner.nextLine();
			}
			
			scanner.close();
		}
		catch(Exception ex)
		{
			System.out.println("Se produjo un problema al tomar los datos de la solución!");
			ex.printStackTrace();
			
			return null;
		}
		
		System.out.println();
		return ret;
	}
}
