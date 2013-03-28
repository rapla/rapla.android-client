/*--------------------------------------------------------------------------*
 | Copyright (C) 2012 Maximilian Lenkeit                                    |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/

package org.rapla.mobile.android.test.utility;

import org.rapla.mobile.android.utility.factory.RaplaContextFactory;

import android.test.AndroidTestCase;

/**
 * RaplaContextFactoryTest
 * 
 * Unit test class for org.rapla.mobile.android.utilities.factory.RaplaContextFactory
 * 
 * @see org.rapla.mobile.android.utility.factory.utilities.factory.RaplaContextFactory
 * @author Maximilian Lenkeit <dev@lenki.com>
 */	
public class RaplaContextFactoryTest extends AndroidTestCase {

	public void testGetInstanceShouldAlwaysReturnTheSameInstance() {
		RaplaContextFactory first = RaplaContextFactory.getInstance();
		RaplaContextFactory second = RaplaContextFactory.getInstance();
		assertEquals(first, second);
	}
}
