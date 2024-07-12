package impronta;

public class Semilla
{
	private String _nombre;
	private double _largo;
	private double _ancho;
	private double _anchoLocacion;
	private double _largoLocacion;
	private double _offsetHorizontalLocacion;
	private double _offsetVerticalLocacion;
	private double _toleranciaLocacion;
	private double _coeficiente;
	
	public Semilla()
	{
	}
	
	public Semilla(double largo, double ancho, double largoInst, double anchoInst, double tolInst, double coef)
	{
		_nombre = "";
		_ancho = ancho;
		_largo = largo;
		_anchoLocacion = anchoInst;
		_largoLocacion = largoInst;
		_toleranciaLocacion = tolInst;
		_offsetHorizontalLocacion = _ancho/2;
		_offsetVerticalLocacion = _largo/2;
		_coeficiente = coef;
	}
	
	public Semilla(String nombre, double largo, double ancho, double largoInst, double anchoInst, double tolInst, double coef)
	{
		_nombre = nombre;
		_ancho = ancho;
		_largo = largo;
		_anchoLocacion = anchoInst;
		_largoLocacion = largoInst;
		_toleranciaLocacion = tolInst;
		_offsetHorizontalLocacion = _largo/2;
		_offsetVerticalLocacion = _ancho/2;
		_coeficiente = coef;
	}
	
	public Semilla(String nombre, double largo, double ancho, double largoInst, double anchoInst, double tolInst, double offsetHorizontal, double offsetVertical, double coef)
	{
		_nombre = nombre;
		_ancho = ancho;
		_largo = largo;
		_anchoLocacion = anchoInst;
		_largoLocacion = largoInst;
		_toleranciaLocacion = tolInst;
		_offsetHorizontalLocacion = offsetHorizontal;
		_offsetVerticalLocacion = offsetVertical;
		_coeficiente = coef;
	}
	
	public String getNombre()
	{
		return _nombre;
	}
	public double getLargo()
	{
		return _largo;
	}
	public double getAncho()
	{
		return _ancho;
	}
	public double getAnchoLocacion()
	{
		return _anchoLocacion;
	}
	public double getLargoLocacion()
	{
		return _largoLocacion;
	}
	public double getToleranciaLocacion()
	{
		return _toleranciaLocacion;
	}
	public double getOffsetHorizontalLocacion()
	{
		return _offsetHorizontalLocacion;
	}
	public double getOffsetVerticalLocacion()
	{
		return _offsetVerticalLocacion;
	}
	public double getCoeficiente()
	{
		return _coeficiente;
	}
	
	public void setLargo(double _largo)
	{
		this._largo = _largo;
	}
	public void setAncho(double _ancho)
	{
		this._ancho = _ancho;
	}
	public void setAnchoLocacion(double _anchoLocacion)
	{
		this._anchoLocacion = _anchoLocacion;
	}
	public void setLargoLocacion(double _largoLocacion)
	{
		this._largoLocacion = _largoLocacion;
	}
	public void setToleranciaLocacion(double _toleranciaLocacion)
	{
		this._toleranciaLocacion = _toleranciaLocacion;
	}
	public void setOffsetHorizontalLocacion(double offset)
	{
		this._offsetHorizontalLocacion = offset;
	}
	public void setOffsetVerticalLocacion(double offset)
	{
		this._offsetVerticalLocacion = offset;
	}
	public void setCoeficiente(double _coeficiente)
	{
		this._coeficiente = _coeficiente;
	}
	
	// Area de la semilla
	public double getArea()
	{
		return _ancho * _largo / 1e6;
	}
	
	// Impresión por consola
	public void mostrar()
	{
		System.out.println("  -> " + _nombre + " - Pad = " + _largo + " x " + _ancho + " - Locacion: " + _largoLocacion + " x " + _anchoLocacion + " +/- " + _toleranciaLocacion + " - Offset locacion: (" + _offsetHorizontalLocacion + ", " + _offsetVerticalLocacion + ") - Coeficiente obj: " + _coeficiente);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(_ancho);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(_anchoLocacion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(_coeficiente);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(_largo);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(_largoLocacion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((_nombre == null) ? 0 : _nombre.hashCode());
		temp = Double.doubleToLongBits(_offsetHorizontalLocacion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(_offsetVerticalLocacion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(_toleranciaLocacion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		Semilla otro = (Semilla) obj;
		return getAncho() == otro.getAncho() && getLargo() == otro.getLargo() &&
			   getAnchoLocacion() == otro.getAnchoLocacion() && getLargoLocacion() == otro.getLargoLocacion() &&
			   getOffsetHorizontalLocacion() == otro.getOffsetHorizontalLocacion() && getOffsetVerticalLocacion() == otro.getOffsetVerticalLocacion() &&
			   getNombre() == otro.getNombre();
	}
	
	
}
