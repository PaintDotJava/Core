// {LICENSE}
/*
 * Copyright 2013-2015 HeroesGrave and other Spade developers.
 * 
 * This file is part of Spade
 * 
 * Spade is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package heroesgrave.spade.core.changes;

import heroesgrave.spade.image.RawImage;
import heroesgrave.spade.image.change.IEditChange;
import heroesgrave.spade.image.change.IMaskChange;
import heroesgrave.spade.io.Serialised;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MoveChange implements IEditChange, IMaskChange, Serialised
{
	public short dx, dy;
	
	public MoveChange()
	{
		
	}
	
	public MoveChange(short dx, short dy)
	{
		this.dx = dx;
		this.dy = dy;
	}
	
	public boolean moved(short dx, short dy)
	{
		if(this.dx == dx && this.dy == dy)
			return false;
		this.dx = dx;
		this.dy = dy;
		return true;
	}
	
	@Override
	public void apply(RawImage image)
	{
		image.move(dx, dy);
	}
	
	@Override
	public MoveChange encode()
	{
		return this;
	}
	
	@Override
	public MoveChange decode()
	{
		return this;
	}
	
	@Override
	public void write(DataOutputStream out) throws IOException
	{
		out.writeShort(dx);
		out.writeShort(dy);
	}
	
	@Override
	public void read(DataInputStream in) throws IOException
	{
		dx = in.readShort();
		dy = in.readShort();
	}
}
