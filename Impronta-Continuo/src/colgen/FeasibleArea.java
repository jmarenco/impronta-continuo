package colgen;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

import impronta.Instancia;
import impronta.Region;
import impronta.Restriccion;

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

			for(Polygon envolvente: _instancia.getRegion().getEnvolventes())
				_region.agregarEnvolvente(areaFactible(envolvente));
			
			for(Polygon agujero: _instancia.getRegion().getAgujeros())
				_region.agregarAgujero(agregarContorno(agujero));

			for(Restriccion restriccion: _instancia.getRestricciones())
				_region.agregarAgujero(agregarContorno(restriccion));

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
			
			double x = vertice.x + (centroide.getX() > vertice.x ? 1 : -1) * minLargoPad() / 2;
			double y = vertice.y + (centroide.getY() > vertice.y ? 1 : -1) * minAnchoPad() / 2;
			
			ret[i] = new Coordinate(x, y);
		}
		
		return _instancia.getFactory().createPolygon(ret);
	}
	
	private Polygon agregarContorno(Restriccion restriccion)
	{
		Coordinate[] vertices = restriccion.getPolygon().getCoordinates();
		Coordinate[] coordinates = new Coordinate[4 * vertices.length];
		
		double offsetHorizontal = minLargoLocacion() / 2;
		double offsetVertical = minAnchoLocacion() / 2;
		
		for(int i=0; i<vertices.length; ++i)
		{
			coordinates[4*i] = new Coordinate(vertices[i].x + offsetHorizontal, vertices[i].y + offsetVertical);
			coordinates[4*i+1] = new Coordinate(vertices[i].x - offsetHorizontal, vertices[i].y + offsetVertical);
			coordinates[4*i+2] = new Coordinate(vertices[i].x + offsetHorizontal, vertices[i].y - offsetVertical);
			coordinates[4*i+3] = new Coordinate(vertices[i].x - offsetHorizontal, vertices[i].y - offsetVertical);
		}
		
		MultiPoint multiPoint = _instancia.getFactory().createMultiPoint(coordinates);
		return (Polygon)multiPoint.getEnvelope();
	}
	
	private Polygon agregarContorno(Polygon agujero)
	{
		Coordinate[] vertices = agujero.getCoordinates();
		Coordinate[] coordinates = new Coordinate[4 * vertices.length];
		
		double offsetHorizontal = minLargoPad() / 2;
		double offsetVertical = minAnchoPad() / 2;
		
		for(int i=0; i<vertices.length; ++i)
		{
			coordinates[4*i] = new Coordinate(vertices[i].x + offsetHorizontal, vertices[i].y + offsetVertical);
			coordinates[4*i+1] = new Coordinate(vertices[i].x - offsetHorizontal, vertices[i].y + offsetVertical);
			coordinates[4*i+2] = new Coordinate(vertices[i].x + offsetHorizontal, vertices[i].y - offsetVertical);
			coordinates[4*i+3] = new Coordinate(vertices[i].x - offsetHorizontal, vertices[i].y - offsetVertical);
		}
		
		MultiPoint multiPoint = _instancia.getFactory().createMultiPoint(coordinates);
		return (Polygon)multiPoint.getEnvelope();
	}
	
	private double minAnchoPad()
	{
		return _instancia.getSemillas().stream().mapToDouble(s -> s.getAncho()).min().orElse(0);
	}
	
	private double minLargoPad()
	{
		return _instancia.getSemillas().stream().mapToDouble(s -> s.getLargo()).min().orElse(0);
	}

	private double minAnchoLocacion()
	{
		return _instancia.getSemillas().stream().mapToDouble(s -> s.getAnchoLocacion()).min().orElse(0);
	}
	
	private double minLargoLocacion()
	{
		return _instancia.getSemillas().stream().mapToDouble(s -> s.getLargoLocacion()).min().orElse(0);
	}
}
