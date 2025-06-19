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
	
	public FeasibleArea(Instancia instancia)
	{
		_instancia = instancia;
	}
	
	public Region get()
	{
		if( _region == null )
		{
			_region = new Region();

			for(Polygon envolvente: _instancia.getRegion().getEnvolventes())
				_region.agregarEnvolvente(areaFactible(envolvente));
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
