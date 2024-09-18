package testing;

import java.util.List;
import java.util.Random;
import java.awt.geom.Point2D;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import impronta.Instancia;
import impronta.Region;
import impronta.Restriccion;
import impronta.Semilla;

public class Generador
{
	private Instancia _instancia;
	private GeometryFactory _factory;
	private Random _random;
	private int _ladosRegion = 15;
	private int _ladosObstaculo = 6;
	
	public Generador(int seed)
	{
		ValtrAlgorithm.initialize(seed);

		_factory = new GeometryFactory();
		_random = new Random(seed);
	}
	
	public Instancia generar(int semillas, int obstaculos, int tamanoSemilla, int tamanoObstaculos)
	{
		Polygon envolvente = poligono(_ladosRegion, 10, 0, 0);
		Region region = new Region();
		region.agregarEnvolvente(envolvente);
		
		_instancia = new Instancia();
		_instancia.setRegion(region);
		
		while( _instancia.getRestricciones().size() < obstaculos)
		{
			Polygon obstaculo = poligono(_ladosObstaculo, tamanoObstaculos, _random.nextDouble() * (10 - tamanoObstaculos), _random.nextDouble() * (10 - tamanoObstaculos));

			if( envolvente.intersects(obstaculo))
				_instancia.agregarRestriccion(new Restriccion("R" + _instancia.getRestricciones().size(), "R" + _instancia.getRestricciones().size(), obstaculo));
		}
		
		_instancia.agregarSemilla(new Semilla(tamanoSemilla, tamanoSemilla/3.0, 0.1, 0.1, 0, 1));
		
		if( semillas > 1 )
			_instancia.agregarSemilla(new Semilla(tamanoSemilla/3.0, tamanoSemilla/7.0, 0.1, 0.1, 0, 1));

		return _instancia;
	}
	
	private Polygon poligono(int lados, double escala, double offsetx, double offsety)
	{
		List<Point2D.Double> puntos = ValtrAlgorithm.generateRandomConvexPolygon(lados);
		Coordinate[] coords = new Coordinate[lados+1];
		
		for(int i=0; i<puntos.size(); ++i)
			coords[i] = new Coordinate(puntos.get(i).x * escala + offsetx, puntos.get(i).y * escala + offsety);
		
		coords[lados] = new Coordinate(puntos.get(0).x * escala + offsetx, puntos.get(0).y * escala + offsety);
		return _factory.createPolygon(coords);
	}
}
