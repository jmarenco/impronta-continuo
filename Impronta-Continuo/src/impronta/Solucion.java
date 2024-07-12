package impronta;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vividsolutions.jts.geom.Coordinate;

// Representa una solución
public class Solucion
{
	private Instancia _instancia;
	private ArrayList<Pad> _pads;
	
	public Solucion(Instancia instancia)
	{
		_instancia = instancia;
		_pads = new ArrayList<Pad>();
	}
	
	public void agregarPad(Pad pad)
	{
		_pads.add(pad);
	}
	
	public ArrayList<Pad> getPads()
	{
		return _pads;
	}
	
	// Area cubierta por la solución, en valor absoluto y en porcentaje del yacimiento
	public double areaCubierta()
	{
		double ret = 0;
		for(Pad pad: getPads())
			ret += pad.getArea();
		
		return ret;
	}
	public double porcentajeCubierto()
	{
		double total = _instancia.getRegion().getArea();
		return total > 0 ? areaCubierta() * 100.0 / total : 0;
	}
	
	// Escribe la solución en un archivo .xml
	public void escribir(String archivoXml)
	{
		try
		{
			System.out.println("Generando archivo de salida: " + archivoXml);
			System.out.println();

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	 
			// Formato con dos decimales
			DecimalFormat df = new DecimalFormat("0.00");
			
			// Raíz
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("Solucion");
			rootElement.setAttribute("Area_cubierta", df.format(areaCubierta()));
			rootElement.setAttribute("Porcentaje_cubierto", df.format(porcentajeCubierto()));
			doc.appendChild(rootElement);
	 
			// Un elemento para cada pad
			for(Pad pad: getPads())
			{
				Element child = doc.createElement("Pad");
				child.setAttribute("Semilla", pad.getSemilla().getNombre());
				child.setAttribute("Centro_X", df.format(pad.getCentro().getX()));
				child.setAttribute("Centro_Y", df.format(pad.getCentro().getY()));
				child.setAttribute("Centro_locacion_X", df.format(pad.getCentroLocacion().getX()));
				child.setAttribute("Centro_locacion_Y", df.format(pad.getCentroLocacion().getY()));
				rootElement.appendChild(child);
				
				Element perimetroPad = doc.createElement("Perimetro_Pad");
				child.appendChild(perimetroPad);
				agregarPuntos(doc, perimetroPad, pad.getPerimetro().getCoordinates());

				Element perimetroLocacion = doc.createElement("Perimetro_Locacion");
				child.appendChild(perimetroLocacion);
				agregarPuntos(doc, perimetroLocacion, pad.getLocacion().getCoordinates());
			}
	 
			// write the content into xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(archivoXml));
	 
			transformer.transform(source, result);

			System.out.println(" -> Archivo generado");
			System.out.println();
		}
		catch(Exception ex)
		{
			System.out.println(" -> Problemas para generar el archivo!");
			System.out.println(ex.getMessage());
			System.out.println();
			
			ex.printStackTrace();
		}
	}
	
	// Agrega los puntos del arreglo al elemento del archivo .xml
	private void agregarPuntos(Document doc, Element nodo, Coordinate[] puntos)
	{
		DecimalFormat df = new DecimalFormat("0.00");
		
		for(int i=0; i<puntos.length; ++i)
		{
			Element punto = doc.createElement("Punto");
			punto.setAttribute("X", df.format(puntos[i].x));
			punto.setAttribute("Y", df.format(puntos[i].y));
			nodo.appendChild(punto);
		}
	}
}
