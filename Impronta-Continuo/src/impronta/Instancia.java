package impronta;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

// Representa una instancia del problema
public class Instancia
{
	private Region _region;
	private double _angulo;
	private double _toleranciaAngulo;
	private double _pasoHorizontal;
	private double _pasoVertical;
	private int _minPuntosHorizontal = 30;
	private int _maxPuntosHorizontal = 200;
	private int _minPuntosVertical = 30;
	private int _maxPuntosVertical = 200;
	private ArrayList<Semilla> _semillas;
	private ArrayList<Restriccion> _restricciones;
	private OGIP _ogip;
	private GeometryFactory _factory;
	private AffineTransform _transform;
	private String _archivo;
	
	public enum Formato { Nada, French, US };
	public static Formato _formato = Formato.Nada;
	public static boolean _alinear = false;
	
	// Constructor por defecto
	public Instancia()
	{
		_semillas = new ArrayList<Semilla>();
		_restricciones = new ArrayList<Restriccion>();
		_factory = new GeometryFactory();
		_ogip = null;
		_archivo = "";
	}
	
	// Connstruye una instancia a partir de un archivo .xml
	public Instancia(String archivoXml)
	{
		_region = new Region();
		_semillas = new ArrayList<Semilla>();
		_restricciones = new ArrayList<Restriccion>();
		_factory = new GeometryFactory();
		_ogip = null;
		_archivo = archivoXml;
		
		System.out.println("Leyendo instancia ...");
		System.out.println();

		try
		{
			File f = new File(archivoXml);
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringElementContentWhitespace(true);
			factory.setIgnoringComments(true);
			    
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document documento = builder.parse(f);
			removeWhitespaceNodes(documento);
			
			if( documento.getChildNodes().getLength() == 0 )
				throw new RuntimeException("Error! El nodo raÃ­z del archivo .xml no tiene hijos!");
				
			Node primero = documento.getChildNodes().item(0);
			
			if( primero.getNodeName() != "Parámetros" )
				throw new RuntimeException("Error! Se esperaba un nodo de tipo 'ParÃ¡metros' como el nodo principal del archivo .xml, y se obtuvo un nodo de tipo '" + documento.getNodeName() + "'");
			
			NodeList hijos = primero.getChildNodes();
			for(int i = 0; i < hijos.getLength(); i++)
			{
				Node nodo = hijos.item(i);
				
				// Compatibilidad hacia atrÃ¡s (hasta la versiÃ³n 0.69)
				if( nodo.getNodeName() == "Área" )
					obtenerArea(nodo);

				// A partir de la versiÃ³n 0.70
				if( nodo.getNodeName() == "Áreas" )
					obtenerAreas(nodo);

				if( nodo.getNodeName() == "Semillas" )
					obtenerSemillas(nodo);

				if( nodo.getNodeName() == "Restricciones" )
					obtenerRestricciones(nodo);

				if( nodo.getNodeName() == "Esfuerzo_horizontal_mínimo" )
					obtenerAngulo(nodo);

				if( nodo.getNodeName() == "Ogip" )
					obtenerOGIP(nodo);
			}
			
			// Calcula los pasos de la discretizaciÃ³n, si corresponde
			calcularPasos();
			
			// Ajusta el Ã¡ngulo, si corresponde
			if( _alinear == true )
				_angulo = 0;
	    }
	    catch (Exception e)
	    {
	    	System.out.println("No se pudo leer el archivo .xml!");
	    	System.out.println(e.getMessage());
	    	e.printStackTrace();
	    }
	}
	
	// Elimina los nodos de texto y espacios en blanco
	private void removeWhitespaceNodes(Node e)
	{
		NodeList children = e.getChildNodes();
		for (int i = children.getLength() - 1; i >= 0; i--)
		{
			Node child = children.item(i);
			if (child instanceof Text && ((Text) child).getData().trim().length() == 0)
			{
				e.removeChild(child);
			}
			else if (child instanceof Node)
			{
				removeWhitespaceNodes((Node) child);
			}
		}
	}
	
	// Auxiliar: Convierte un String a double usando la especificaciÃ³n de formato, si corresponde
	private double toDouble(String s) throws ParseException
	{
		NumberFormat nf = null;
		
		if( _formato == Formato.French && s.contains("."))
			throw new ParseException("No se pudo convertir " + s + " a un valor numerico.", 0);
		
		if( _formato == Formato.US && s.contains(","))
			throw new ParseException("No se pudo convertir " + s + " a un valor numerico.", 0);

		if( _formato == Formato.French )
			nf = NumberFormat.getInstance(Locale.FRENCH);
		else if( _formato == Formato.US )
			nf = NumberFormat.getInstance(Locale.US);

		return nf != null ? nf.parse(s).doubleValue() : Double.parseDouble(s);
	}	
	
	// Obtiene varias Ã¡reas del archivo .xml
	private void obtenerAreas(Node nodo)
	{
		try
		{
			NodeList hijos = nodo.getChildNodes();
			for(int i = 0; i < hijos.getLength(); i++)
			{
				if( hijos.item(i).getNodeName() == "Área")
					obtenerArea(hijos.item(i));
			}
		}
	    catch (Exception e)
	    {
	    	System.out.println("No se pudieron leer los datos de las areas!");
	    	System.out.println(e.getMessage());
	    	e.printStackTrace();
	    }
	}
	
	// Obtiene el Ã¡rea del archivo .xml
	private void obtenerArea(Node nodo)
	{
		try
		{
			String yacimiento = nodo.getAttributes().getNamedItem("Capa").getNodeValue();
			String id = nodo.getAttributes().getNamedItem("ID").getNodeValue();
			
			System.out.println("Yacimiento: " + yacimiento + " - ID: " + id);
			System.out.println();
			
			NodeList hijos = nodo.getChildNodes();

			if( hijos.getLength() > 0 && hijos.item(0).getNodeName() == "Vértice" )
			{
				// Compatibilidad hacia atras, con el formato anterior a la versiÃ³n 0.70 
				leerPoligono(hijos, true);
			}
			else
			{
				// A partir de la versiÃ³n 0.70, lista de envolventes y de agujeros
				for(int i = 0; i < hijos.getLength(); i++)
				{
					if( hijos.item(i).getNodeName() == "Envolvente")
						leerPoligono(hijos.item(i).getChildNodes(), true);
					
					if( hijos.item(i).getNodeName() == "Agujero")
						leerPoligono(hijos.item(i).getChildNodes(), false);
				}
			}
		}
	    catch (Exception e)
	    {
	    	System.out.println("No se pudieron leer los datos del yacimiento!");
	    	System.out.println(e.getMessage());
	    	e.printStackTrace();
	    }
	}
	
	// Agrega una envolvente o un agujero a la regiÃ³n
	private void leerPoligono(NodeList hijos, boolean envolvente) throws ParseException
	{
		Coordinate[] coords = new Coordinate[hijos.getLength()];
		
		for(int i = 0; i < hijos.getLength(); i++)
		{
			String x = hijos.item(i).getAttributes().getNamedItem("X").getNodeValue();
			String y = hijos.item(i).getAttributes().getNamedItem("Y").getNodeValue();
			
			System.out.println("  -> x = " + toDouble(x) + ", y = " + toDouble(y) + (envolvente ? " (+)" : " (-)"));
			
			coords[i] = toCoordinate(toDouble(x), toDouble(y));
		}
		
		if( envolvente == true )
			_region.agregarEnvolvente(_factory.createPolygon(coords));
		else
			_region.agregarAgujero(_factory.createPolygon(coords));

		System.out.println();
	}
	
	// Genera una coordenada, rotando el punto si corresponde
	private Coordinate toCoordinate(double x, double y)
	{
		Point2D original = new Point2D.Double(x, y);
		
		if( _alinear == true )
		{
			Point2D transformado = new Point2D.Double(x, y);
			_transform.transform(original, transformado);
			original = transformado;
		}
			
		return new Coordinate(original.getX(), original.getY());
	}
	
	// Obtiene las semillas del archivo .xml
	private void obtenerSemillas(Node nodo)
	{
		try
		{
			System.out.println("Leyendo semillas");
			System.out.println();
			
			NodeList hijos = nodo.getChildNodes();
			for(int i = 0; i < hijos.getLength(); i++)
			{
				Node semilla = hijos.item(i);
				NodeList valores = semilla.getChildNodes();
				
				String nombre = semilla.getAttributes().getNamedItem("ID").getNodeValue();
				String coeficiente = semilla.getAttributes().getNamedItem("Coeficiente").getNodeValue();
				String largoPad = "0";
				String anchoPad = "0";
				String anchoLocacion = "0";
				String largoLocacion = "0";
				String offsetHorizontal = "";
				String offsetVertical = "";
				String toleranciaLocacion = "0";

				for(int j = 0; j < valores.getLength(); j++)
				{
					Node valor = valores.item(j);
					if( valor.getNodeName() == "PAD" )
					{
						largoPad = valor.getAttributes().getNamedItem("Largo").getNodeValue(); 
						anchoPad = valor.getAttributes().getNamedItem("Ancho").getNodeValue(); 
					}

					if( valor.getNodeName() == "LOCACION" )
					{
						largoLocacion = valor.getAttributes().getNamedItem("Largo").getNodeValue(); 
						anchoLocacion = valor.getAttributes().getNamedItem("Ancho").getNodeValue(); 
						toleranciaLocacion = valor.getAttributes().getNamedItem("Tolerancia").getNodeValue(); 
						
						try
						{
							// Cruzados a partir de la versiÃ³n 0.69
							offsetHorizontal = valor.getAttributes().getNamedItem("Y").getNodeValue();
							offsetVertical = valor.getAttributes().getNamedItem("X").getNodeValue();
						}
						catch(Exception e)
						{
							offsetHorizontal = "";
							offsetVertical = "";
						}
					}
				}
				
				Semilla nueva = new Semilla(nombre, toDouble(largoPad), toDouble(anchoPad), toDouble(largoLocacion), toDouble(anchoLocacion), toDouble(toleranciaLocacion), toDouble(coeficiente));
				if( offsetHorizontal.length() > 0 && offsetVertical.length() > 0 )
				{
					nueva.setOffsetHorizontalLocacion(toDouble(offsetHorizontal));
					nueva.setOffsetVerticalLocacion(toDouble(offsetVertical));
				}
					
				nueva.mostrar();
				_semillas.add(nueva);
			}

			System.out.println();
		}
	    catch (Exception e)
	    {
	    	System.out.println("No se pudieron leer los datos de las semillas!");
	    	System.out.println(e.getMessage());
	    	e.printStackTrace();
	    }
	}
	
	// Calcula valores por defecto para la discretizaciÃ³n
	private void calcularPasos()
	{
		if( _pasoHorizontal == 0 )
		{
			// Proyecta la regiÃ³n al eje x
			double xMax = Double.NEGATIVE_INFINITY;
			double xMin = Double.POSITIVE_INFINITY;
			
			for(Polygon envolvente: _region.getEnvolventes())
			for(Coordinate c: envolvente.getCoordinates())
			{
				xMax = Math.max(xMax, c.x);
				xMin = Math.min(xMin, c.x);
			}

			// Calcula el mcd entre los anchos de las semillas
			ArrayList<Integer> valores = new ArrayList<Integer>();
			for(Semilla semilla: _semillas)
				valores.add((int)semilla.getLargo());
			
			_pasoHorizontal = mcd(valores);

			// Correcciones
			if( _pasoHorizontal == 1 ) // Si son coprimos ...
				_pasoHorizontal = Collections.max(valores) / 10;

			while( Collections.min(valores) / _pasoHorizontal < 5 )
				_pasoHorizontal = (int)(_pasoHorizontal / 2);
			
			while( (xMax - xMin) / _pasoHorizontal < _minPuntosHorizontal )
				_pasoHorizontal = (int)(_pasoHorizontal / 2);
			
			while( (xMax - xMin) / _pasoHorizontal > _maxPuntosHorizontal )
				_pasoHorizontal *= 2;
		}
		
		if( _pasoVertical == 0 )
		{
			// Proyecta la regiÃ³n al eje y
			double yMax = Double.NEGATIVE_INFINITY;
			double yMin = Double.POSITIVE_INFINITY;
			
			for(Polygon envolvente: _region.getEnvolventes())
			for(Coordinate c: envolvente.getCoordinates())
			{
				yMax = Math.max(yMax, c.y);
				yMin = Math.min(yMin, c.y);
			}

			// Calcula el mcd entre los largos de las semillas
			ArrayList<Integer> valores = new ArrayList<Integer>();
			for(Semilla semilla: _semillas)
				valores.add((int)semilla.getAncho());
			
			_pasoVertical = mcd(valores);
			
			// Correcciones
			if( _pasoVertical == 1 ) // Si son coprimos ...
				_pasoVertical = Collections.max(valores) / 10;
			
			while( Collections.min(valores) / _pasoVertical < 5 )
				_pasoVertical = (int)(_pasoVertical / 2);

			while( (yMax - yMin) / _pasoVertical < _minPuntosVertical )
				_pasoVertical = (int)(_pasoVertical / 2);
			
			while( (yMax - yMin) / _pasoVertical > _maxPuntosVertical )
				_pasoVertical *= 2;
		}
	}
	
	// Auxiliar: MÃ¡ximo comÃºn divisor
	private int mcd(ArrayList<Integer> valores)
	{
		if( valores.size() == 0 )
			return 0;
		
		int a = valores.get(0);
		for(int i=1; i<valores.size(); ++i)
			a = mcd(a, valores.get(i));
		
		return a;
	}
	private int mcd(int a, int b)
	{
		return b ==0  ? a : mcd(b, a%b);
	}
	
	// Obtiene las restricciones del archivo .xml
	private void obtenerRestricciones(Node nodo)
	{
		try
		{
			System.out.println("Leyendo Restricciones");
			System.out.println();
			
			String id = "";
			String ring = "";
			
			NodeList hijos = nodo.getChildNodes();
			for(int i = 0; i < hijos.getLength(); i++)
			{
				try
				{
					Node restriccion = hijos.item(i);
					NodeList puntos = restriccion.getChildNodes();
					Coordinate[] coords = new Coordinate[puntos.getLength()];
	
					id = restriccion.getAttributes().getNamedItem("ID").getNodeValue();
					ring = restriccion.getAttributes().getNamedItem("Ring").getNodeValue();
	
					for(int j = 0; j < puntos.getLength(); j++)
					{
						String x = puntos.item(j).getAttributes().getNamedItem("X").getNodeValue();
						String y = puntos.item(j).getAttributes().getNamedItem("Y").getNodeValue();
						
						coords[j] = toCoordinate(toDouble(x), toDouble(y));
					}
					
					System.out.println("  -> Restriccion: ID " + id + ", Ring " + ring + " = " + coords.length + " puntos");
					_restricciones.add(new Restriccion(id, ring, _factory.createPolygon(coords)));
				}
				catch(Exception e)
				{
					System.out.println();
					System.out.println("  -> Restriccion: ID " + id + ", Ring " + ring + " - No se pudo generar la restricciÃ³n!");
					System.out.println("  -> Error: " + e.getMessage());
					System.out.println();
				}
				
				id = "";
				ring = "";
			}

			System.out.println();
		}
	    catch (Exception e)
	    {
	    	System.out.println("No se pudieron leer las restricciones!");
	    	System.out.println(e.getMessage());
	    	e.printStackTrace();
	    }
	}	
	
	// Obtiene el Ã¡rea del archivo .xml
	private void obtenerAngulo(Node nodo)
	{
		try
		{
			String angulo = nodo.getAttributes().getNamedItem("Ángulo").getNodeValue();
			String tolerancia = nodo.getAttributes().getNamedItem("Tolerancia").getNodeValue();
			String pasoHorizontal = nodo.getAttributes().getNamedItem("Nx").getNodeValue();
			String pasoVertical = nodo.getAttributes().getNamedItem("Ny").getNodeValue();
			String maxGeneracion = nodo.getAttributes().getNamedItem("MaxTiempoModelo").getNodeValue();
			String maxSolver = nodo.getAttributes().getNamedItem("MaxTiempoSolver").getNodeValue();
			
			_angulo = toDouble(angulo) * Math.PI / 180;
			_toleranciaAngulo = toDouble(tolerancia) * Math.PI / 180;
			_pasoHorizontal = toDouble(pasoHorizontal);
			_pasoVertical = toDouble(pasoVertical);
			
			Timer.setTiempoGeneracion(toDouble(maxGeneracion));
			Timer.setTiempoSolver(toDouble(maxSolver));
			
			DecimalFormat df = new DecimalFormat("0.0000");
			System.out.println("Parametros de la optimizacion");
			System.out.println();
			System.out.println("  -> Esfuerzo minimo: " + df.format(_angulo) + " +/- " + df.format(_toleranciaAngulo));
			System.out.println("  -> Delta x: " + df.format(_pasoHorizontal) + ", Delta y: " + df.format(_pasoVertical) + " (input)");
			System.out.println("  -> Tiempo maximo generacion: " + df.format(Timer.getTiempoGeneracion()) + " sg., solver: " + df.format(Timer.getTiempoSolver()) + " sg.");
			System.out.println();
			
			if( _alinear == true )
				_transform = AffineTransform.getRotateInstance(-_angulo, 0, 0);
		}
	    catch (Exception e)
	    {
	    	System.out.println("No se pudieron leer los parametros!");
	    	System.out.println(e.getMessage());
	    	e.printStackTrace();
	    }
	}	
	
	// Obtiene las mediciones de OGIP del archivo .xml
	private void obtenerOGIP(Node nodo)
	{
		try
		{
			System.out.println("Leyendo OGIP");
			System.out.println();
			
			_ogip = new OGIP();
			
			NodeList hijos = nodo.getChildNodes();
			for(int i = 0; i < hijos.getLength(); i++)
			{
				try
				{
					Node medicion = hijos.item(i);
	
					String x = medicion.getAttributes().getNamedItem("X").getNodeValue();
					String y = medicion.getAttributes().getNamedItem("Y").getNodeValue();
					String valor = medicion.getAttributes().getNamedItem("Valor").getNodeValue();
					
					_ogip.agregar(toCoordinate(toDouble(x), toDouble(y)), toDouble(valor));
				}
				catch(Exception e)
				{
					System.out.println("  -> Error al leer la medicion de OGIP numero " + i + ": " + e.getMessage());
				}
			}
		}
	    catch (Exception e)
	    {
	    	System.out.println("No se pudieron leer las mediciones de OGIP!");
	    	System.out.println(e.getMessage());
	    	e.printStackTrace();
	    }
		
		System.out.println("  -> " + _ogip.getCantidad() + " mediciones leidas");
		System.out.println();
	}	
	
	// Setters
	public void setRegion(Region region)
	{
		if( region == null )
			throw new IllegalArgumentException();
			
		_region = region;
	}
	public void setAngulo(double angulo)
	{
		if( angulo < 0 || angulo >= 2*Math.PI)
			throw new IllegalArgumentException();
		
		_angulo = angulo;
	}
	public void setToleranciaAngulo(double tolerancia)
	{
		if( tolerancia < 0 )
			throw new IllegalArgumentException();
		
		_toleranciaAngulo = tolerancia;
		
	}
	public void setPasoHorizontal(double paso)
	{
		if( paso <= 0 )
			throw new IllegalArgumentException();

		_pasoHorizontal = paso;
	}
	public void setPasoVertical(double paso)
	{
		if( paso <= 0 )
			throw new IllegalArgumentException();

		_pasoVertical = paso;
	}
	public void agregarSemilla(Semilla s)
	{
		_semillas.add(s);
	}
	
	// Getters
	public Region getRegion()
	{
		return _region;
	}
	public ArrayList<Semilla> getSemillas()
	{
		return _semillas;
	}
	public Semilla getSemilla(int i)
	{
		return _semillas.get(i);
	}
	public ArrayList<Restriccion> getRestricciones()
	{
		return _restricciones;
	}
	public OGIP getOGIP()
	{
		return _ogip;
	}
	public double getAngulo()
	{
		return _angulo;
	}
	public double getToleranciaAngulo()
	{
		return _toleranciaAngulo;
	}
	public double getPasoHorizontal()
	{
		return _pasoHorizontal;
	}
	public double getPasoVertical()
	{
		return _pasoVertical;
	}
	public String getArchivo()
	{
		return _archivo;
	}
	
	// Obtiene un constructor de geometrias
	public GeometryFactory getFactory()
	{
		return _region.getFactory();
	}
}
