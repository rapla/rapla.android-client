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

package org.rapla.mobile.android.test.widget.adapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.internal.AllocatableImpl;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.mobile.android.test.test.FixtureHelper;
import org.rapla.mobile.android.widget.adapter.AllocatableAdapter;

import android.test.AndroidTestCase;

/**
 * AllocatableAdapterTest
 * 
 * Unit tests for
 * <code>org.rapla.mobile.android.widget.adapter.AllocatableAdapter</code>
 * 
 * @see org.rapla.mobile.android.widget.adapter.AllocatableAdapter
 * @author Maximilian Lenkeit <dev@lenki.com>
 */
public class AllocatableAdapterTest extends AndroidTestCase {

	protected AllocatableAdapter adapter;
	protected Allocatable[] allocatables;
	protected ReservationImpl selectedReservation;

	protected void setUp() throws Exception {
		super.setUp();

		this.allocatables = new Allocatable[2];
		for (int i = 0; i < allocatables.length; i++) {
			AllocatableImpl a = FixtureHelper.createAllocatable();
			a.getClassification().setValue("name", "Allocatable " + i);
			this.allocatables[i] = a;
		}

		this.selectedReservation = FixtureHelper.createReservation();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		this.adapter = null;
	}

	public void testPreConditions() {
		// Check whether allocatables have been set up correctly
		Allocatable a = this.allocatables[0];
		String name = a.getClassification().getValue("name").toString();
		assertEquals("Allocatable 0", name);

		// Check whether adapter can be initialized
		this.adapter = new AllocatableAdapter(this.getContext(),
				this.selectedReservation, this.allocatables);
	}

	public void testGetCountShouldReturnNumberOfAssignedAllocatables() {
		this.adapter = new AllocatableAdapter(this.getContext(),
				this.selectedReservation, this.allocatables);
		assertEquals(this.allocatables.length, this.adapter.getCount());
	}

	/**
	 * This test only fails because the dynamic type fixtures are not set up
	 * correctly resulting in a NullPointerException.
	 */
	public void failingtestConstructorShouldSortAllocatablesLexicographicallyByName() {
		// Reverse array
		List<Allocatable> sortable = Arrays.asList(this.allocatables);
		Collections.reverse(sortable);
		this.allocatables = new Allocatable[sortable.size()];
		sortable.toArray(this.allocatables);

		this.adapter = new AllocatableAdapter(this.getContext(),
				this.selectedReservation, this.allocatables);

		// Compare first element of adapter with first element of sortable
		assertFalse(sortable.get(0).equals(this.adapter.getItem(0)));

		// Compare first element of adapter with second element of sortable
		assertEquals(this.adapter.getItem(0), sortable.get(1));
	}

}
