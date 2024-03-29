//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package parser.type;

import param.BigRational;
import parser.EvaluateContext.EvalMode;
import prism.PrismLangException;

public abstract class Type 
{
	/**
	 * Returns the string denoting this type, e.g. "int", "bool".
	 */
	public abstract String getTypeString();
	
	/**
	 * Is this a primitive type (bool, int, etc.)?
	 */
	public boolean isPrimitive()
	{
		// Assume true by default; override if not
		return true;
	}
	
	/**
	 * Returns the default value for this type, assuming no initialisation specified.
	 */
	public Object defaultValue()
	{
		// Play safe: assume null
		return null;
	}
	
	/**
	 * Returns true iff a variable of this type can be assigned a value that is of type {@code type}. 
	 */
	public boolean canAssign(Type type)
	{
		// Play safe: assume not possible, unless explicitly overridden.
		return false;
	}
	
	/**
	 * Make sure that a value, stored as an Object (Integer, Boolean, etc.)
	 * is the correct kind of Object for this type.
	 * Basically, implement some implicit casts (e.g. from type int to double).
	 * The evaluation mode is not changed (e.g. when casting  int to double,
	 * the conversion could be either Integer -> Double or BigInteger -> BigRational).
	 * This should only only work for combinations of types that satisfy {@code #canAssign(Type)}.
	 * If not, an exception is thrown (but such problems should have been caught earlier by type checking)
	 */
	public Object castValueTo(Object value) throws PrismLangException
	{
		// Play safe: assume error unless explicitly overridden.
		throw new PrismLangException("Cannot cast a value to type " + getTypeString());
	}

	/**
	 * Make sure that a value, stored as an Object (Integer, Boolean, etc.),
	 * is the correct kind of Object for this type, and a given evaluation mode.
	 * E.g. a "double" is stored as a Double for floating point mode (EvalMode.FP)
	 * but a BigRational for exact mode (EvalMode.EXACT).
	 * Basically, implement some implicit casts (e.g. from type int to double)
	 * and some conversions between evaluation modes (e.g. BigRational to Double).
	 * This should only only work for combinations of types that satisfy {@code #canAssign(Type)}.
	 * If not, an exception is thrown (but such problems should have been caught earlier by type checking)
	 */
	public Object castValueTo(Object value, EvalMode evalMode) throws PrismLangException
	{
		// Play safe: assume error unless explicitly overridden.
		throw new PrismLangException("Cannot cast a value to type " + getTypeString());
	}

	/**
	 * Cast a BigRational value to the Java data type (Boolean, Integer, Double, ...)
	 * corresponding to this type.
	 * <br>
	 * For boolean and integer, this throws an exception if the value can not be
	 * precisely represented by the Java data type; for double, loss of precision
	 * is expected and does not raise an exception.
	 */
	public Object castFromBigRational(BigRational value) throws PrismLangException
	{
		// Play safe: assume error unless explicitly overridden.
		throw new PrismLangException("Cannot cast rational number to type " + getTypeString());
	}

	@Override
	public String toString()
	{
		return getTypeString();
	}
}
