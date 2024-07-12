package impronta;

import impronta.Grafo.Clique;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class SolverHeuristico extends SolverDiscreto
{
	// Generador de números aleatorios
	private static Random _random = new Random();
	
	// Constructor
	public SolverHeuristico(Instancia instancia)
	{
		super(instancia);
	}
	
	// Resuelve la instancia
	public Solucion resolver()
	{
		construirDiscretizacion();
		generarPads();
		construirGrafo();

		return construirSolucion();
	}
	
	// Construye una solución en forma golosa y semi-aleatoria
	public Solucion construirSolucion()
	{
		System.out.println("Construyendo solución ...");
		System.out.println();
		
		Solucion ret = null;
		double mejorValor = Double.MIN_VALUE;
		int cantidadTotal = 100;
		
		for(int i=0; i<cantidadTotal; ++i)
		{
			Solucion actual = new Solucion(_instancia);
			double valorizacion = 0;
	
			// Vértices en orden descendente de valorización
			boolean[] pendientes = todosVerdaderos(_grafo.getVertices());
			ArrayList<Vertice> vertices = obtenerVertices(pendientes);
			
			// Mientras haya vértices para seleccionar ...
			while( vertices.size() > 0 )
			{
				// Selecciona entre los mejores en cuanto a la función objetivo
				int k = _random.nextInt(Math.min(Math.max(10, vertices.size() / 10), vertices.size()));
				Vertice v = vertices.get(k);
	
				// Lo agrega a la solución
				actual.agregarPad(_pads.get(v.numero));
				
				// Elimina de los posibles a todos los pads que intersecan al pad seleccionado
				for(Clique clique: _grafo.getCliquesDe(v.numero))
				for(Integer vecino: clique)
					pendientes[vecino] = false;
	
				vertices = obtenerVertices(pendientes);
				valorizacion += v.valorizacion;
			}
			
			System.out.print("  -> Solución " + (i+1) + "/" + cantidadTotal + " - fobj: " + valorizacion);

			if( mejorValor < valorizacion )
			{
				ret = actual;
				mejorValor = valorizacion;
				System.out.print(" *");
			}
			
			System.out.println();			
		}
		
		return ret;
	}
	
	// Retorna un arreglo con todos los vértices en verdadero
	public boolean[] todosVerdaderos(int n)
	{
		boolean[] ret = new boolean[n];
		
		for(int i=0; i<n; ++i)
			ret[i] = true;
		
		return ret;
	}
	
	// Representa un vértice para la construcción de la solución
	private class Vertice implements Comparable<Vertice>
	{
		public int numero;
		public double valorizacion;
		
		public Vertice(int n, double v)
		{
			numero = n;
			valorizacion = v;
		}
		
		@Override public int hashCode()
		{
			return numero % 32;
		}

		@Override public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			if (numero != ((Vertice)obj).numero) return false;
			return true;
		}

		@Override public int compareTo(Vertice otro)
		{
			return valorizacion < otro.valorizacion ? 1 : (valorizacion == otro.valorizacion ? 0 : -1);
		}
	}
	
	// Construye un arreglo auxiliar de vértices
	private ArrayList<Vertice> obtenerVertices(boolean[] habilitados)
	{
		ArrayList<Vertice> ret = new ArrayList<Vertice>(_grafo.getVertices());
		
		for(int i=0; i<_grafo.getVertices(); ++i) if( habilitados[i] )
			ret.add(new Vertice(i, _grafo.getPeso(i)));
		
		Collections.sort(ret);
		return ret;
	}
}
