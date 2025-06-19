package colgen;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import impronta.Instancia;
import impronta.Pad;
import impronta.Region;
import impronta.Semilla;

public class CubrimientoDual
{
	private Instancia _instancia;
	private Map<Point, Double> _cubrimiento;
	private Map<Pad, Double> _pads;
	
	public CubrimientoDual(Instancia instancia, Map<Point, Double> cubrimiento)
	{
		_instancia = instancia;
		_cubrimiento = cubrimiento;
		_pads = new HashMap<Pad, Double>();
		
		for(Point point: _cubrimiento.keySet()) if( _cubrimiento.get(point) > 0.01 )
		for(Semilla semilla: _instancia.getSemillas())
			_pads.put(Pad.rigido(_instancia, semilla, new Coordinate(point.getX(), point.getY())), _cubrimiento.get(point));
	}
	
	public static List<Point> getUncoveredPoints(Instancia instancia, Map<Point, Double> cubrimiento)
	{
		return new CubrimientoDual(instancia, cubrimiento).getUncoveredPoints();
	}
	
	public static Map<Semilla, Region> get(Instancia instancia, Map<Point, Double> cubrimiento)
	{
		return new CubrimientoDual(instancia, cubrimiento).get();
	}
	
	public List<Point> getUncoveredPoints()
	{
		List<Point> ret = new ArrayList<Point>();
		Map<Semilla, Region> regiones = this.get();
		
		for(Semilla semilla: regiones.keySet())
		{
			Geometry geometry = regiones.get(semilla).getGeometry();
			
			for(int i=0; i<geometry.getNumGeometries(); ++i)
			{
				Coordinate[] coords = geometry.getGeometryN(i).getCoordinates();
				boolean alguno = false;
				
				for(int j=0; j<coords.length; ++j)
				{
					Point point = _instancia.getFactory().createPoint(coords[j]);
					if( _cubrimiento.keySet().contains(point) == false && ret.contains(point) == false )
					{
						ret.add(point);
						alguno = true;
					}
				}
				
				if( alguno == false )
					ret.add(geometry.getGeometryN(i).getCentroid());
			}
		}
		
		return ret;
	}
	
	public Map<Semilla, Region> get()
	{
		Map<Semilla, Region> ret = new HashMap<Semilla, Region>();
		for(Semilla semilla: _instancia.getSemillas())
		{
			Region region = FeasibleArea.get(_instancia);
			for(Pad pad: _pads.keySet()) if( pad.getSemilla() == semilla )
			{
				List<Pad> intersecting = _pads.keySet().stream().filter(p -> p != pad && p.getSemilla() == semilla && p.interseca(pad)).collect(Collectors.toList());
				procesar(region, pad.getPerimetro(), intersecting, 0, _pads.get(pad));
			}
			
			ret.put(semilla, region);
		}

		return ret;
	}
	
	private void procesar(Region region, Geometry covered, List<Pad> intersecting, int inicial, double lhs)
	{
		if( inicial == intersecting.size() )
		{
			if( lhs >= 0.99 )
			{
				for(int i=0; i<covered.getNumGeometries(); ++i)
					region.agregarAgujero((Polygon)covered.getGeometryN(i));
			}
		}
		else
		{
			procesar(region, covered, intersecting, inicial+1, lhs);
			procesar(region, covered.difference(intersecting.get(inicial).getPerimetro()), intersecting, inicial+1, lhs + _pads.get(intersecting.get(inicial)));
		}
	}
}
