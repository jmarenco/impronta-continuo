package colgen;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import impronta.Instancia;
import impronta.Region;

public class FeasibleArea
{
	private Instancia _instancia;
	private Region _region;
	
	// Cache
	private static Instancia _instanciaCache;
	private static Region _regionCache;
	
	public FeasibleArea(Instancia instancia)
	{
		_instancia = instancia;
	}
	
	public static Region get(Instancia instancia)
	{
		return new FeasibleArea(instancia).get();
	}
	
	public Region get()
	{
		if( _instanciaCache == _instancia && _regionCache != null )
			return _regionCache;
		
		if( _region == null )
		{
			_region = new Region();

			// Bugfix: Eliminar los agujeros y las restricciones
			for(Polygon envolvente: _instancia.getRegion().getEnvolventes())
				_region.agregarEnvolvente(areaFactible(envolvente));

			_instanciaCache = _instancia;
			_regionCache = _region;
		}
		
		return _region;
	}
	
	private Polygon areaFactible(Polygon envolvente)
	{
		Coordinate[] vertices = envolvente.getCoordinates();
		Coordinate[] ret = new Coordinate[vertices.length];
		Point centroide = envolvente.getCentroid();
		
		for(int i=0; i<vertices.length; ++i)
		{
			Coordinate vertice = vertices[i];
			
			double x = vertice.x + (centroide.getX() > vertice.x ? 1 : -1) * minLargo() / 2;
			double y = vertice.y + (centroide.getY() > vertice.y ? 1 : -1) * minAncho() / 2;
			
			ret[i] = new Coordinate(x, y);
		}
		
		return _instancia.getFactory().createPolygon(ret);
	}
	
	private double minAncho()
	{
		return _instancia.getSemillas().stream().mapToDouble(s -> s.getAncho()).min().orElse(0);
	}
	
	private double minLargo()
	{
		return _instancia.getSemillas().stream().mapToDouble(s -> s.getLargo()).min().orElse(0);
	}
}
