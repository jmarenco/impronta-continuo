package impronta;

import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class Region
{
	private ArrayList<Polygon> _envolventes;
	private ArrayList<Polygon> _agujeros;
	
	public Region()
	{
		_envolventes = new ArrayList<Polygon>();
		_agujeros = new ArrayList<Polygon>();
	}
	
	public void agregarEnvolvente(Polygon polygon)
	{
		_envolventes.add(polygon);
	}

	public void agregarAgujero(Polygon polygon)
	{
		_agujeros.add(polygon);
	}
	
	public ArrayList<Polygon> getEnvolventes()
	{
		return _envolventes;
	}
	
	public ArrayList<Polygon> getAgujeros()
	{
		return _agujeros;
	}
	
	public double getArea()
	{
		return getGeometry().getArea();
	}
	
	// Obtiene la figura dada por la union de las envolventes menos la union de los agujeros
	public Geometry getGeometry()
	{
		Geometry ret = null;

		for(Polygon envolvente: _envolventes)
		{
			if( ret == null )
				ret = envolvente;
			else
				ret = ret.union(envolvente);
		}
		
		for(Polygon agujero: _agujeros)
			ret = ret.difference(agujero);
		
		return ret;
	}
	
	// Obtiene un constructor de geometrías
	public GeometryFactory getFactory()
	{
		if( _envolventes.size() > 0 )
			return _envolventes.get(0).getFactory();
		
		if( _agujeros.size() > 0 )
			return _agujeros.get(0).getFactory();
		
		throw new RuntimeException("Error: Region.getFactory(), no hay envolventes ni agujeros registrados en la region!");
	}

	// Clonación
	public Region clonar()
	{
		Region ret = new Region();
		
		for(Polygon envolvente: _envolventes)
			ret.agregarEnvolvente((Polygon)envolvente.clone());

		for(Polygon agujero: _agujeros)
			ret.agregarAgujero((Polygon)agujero.clone());

		return ret;
	}
}
