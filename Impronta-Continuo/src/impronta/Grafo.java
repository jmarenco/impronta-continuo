package impronta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Grafo
{
	// Cantidad de vértices
	private int _vertices;
	
	// Pesos de los vértices
	private double[] _pesos;
	
	// Representa una clique
	public class Clique extends HashSet<Integer>
	{
		private static final long serialVersionUID = 1L;
		public int id;
	}
	
	// Conjunto de todas las cliques
	private LinkedList<Clique> _cliques;
	
	// Cliques que tocan a cada vértice
	private ArrayList<LinkedList<Clique>> _cliquesDe;
	
	// Constructor
	public Grafo(int vertices)
	{
		_vertices = vertices;
		_pesos = new double[_vertices];
		_cliques = new LinkedList<Clique>();
		_cliquesDe = new ArrayList<LinkedList<Clique>>(_vertices);
		
		for(int i=0; i<_vertices; ++i)
			_cliquesDe.add(new LinkedList<Clique>());		
	}
	
	// Agrega una clique
	public void agregarClique(Set<Integer> conjunto)
	{
		Clique clique = new Clique();
		clique.id = _cliques.size();
		
		for(Integer i: conjunto)
			clique.add(i);
		
		_cliques.add(clique);
		
		for(Integer i: clique)
			_cliquesDe.get(i).add(clique);
	}
	
	// Consulta la cantidad de vértices
	public int getVertices()
	{
		return _vertices;
	}
	
	// Consulta las cliques
	public List<Clique> getCliques()
	{
		return _cliques;
	}
	public List<Clique> getCliquesDe(int vertice)
	{
		return _cliquesDe.get(vertice);
	}
	
	// Pesos
	public void setPeso(int vertice, double peso)
	{
		_pesos[vertice] = peso;
	}
	public double getPeso(int vertice)
	{
		return _pesos[vertice];
	}
}
