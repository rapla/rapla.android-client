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

package org.rapla.mobile.android.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.mobile.android.R;
import org.rapla.mobile.android.widget.adapter.AllocatableAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Toast;

/**
 * The allocatable details screen allows the user to book an allocatable of the
 * previously selected allocatable category. A booked allocatable can be
 * assigned to any number of appointments.
 * 
 * @author Maximilian Lenkeit <dev@lenki.com>
 */
public class AllocatableDetailsActivity extends BaseActivity {

	private ListView allocatableListView;
	private static final int DIALOG_CONFIRM_UNDO_BOOKING = 1;
	private static final int DIALOG_ASSIGN_APPOINTMENTS = 2;
	private AsyncTask<?, ?, ?> runningTask;
	public static final String INTENT_STRING_ALLOCATABLE_CATEGORY_ELEMENT_KEY = "element_key";
	private SelectedAllocatableActionHandler selectedAllocatableActionHandler = new SelectedAllocatableActionHandler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set content and custom title
		this.setContentView(R.layout.allocatable_details_list);
		this.setTitle(R.string.titlebar_title_allocatable_list);

		// Initialize references to view
		this.allocatableListView = (ListView) findViewById(R.id.allocatable_details_list);
		this.allocatableListView.setEmptyView(findViewById(android.R.id.empty));
		this.allocatableListView
				.setOnItemClickListener(new AllocatableListItemClickedListener());

		// Register list view and list items for context menu
		this.registerForContextMenu(this.allocatableListView);
	}

	public void onDestroy() {
		super.onDestroy();

		// Stop running background task if available. This is e.g. necessary to
		// avoid short dump when rotating device.
		if (this.runningTask != null) {
			this.runningTask.cancel(true);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		// Context menu for allocatable list item
		if (v.getId() == R.id.allocatable_details_list) {

			// Get info about selected list item and initialize action handler
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			this.selectedAllocatableActionHandler.handleListItem(info.position);

			// Context menu for list item
			menu.setHeaderTitle(R.string.options);
			if (this.selectedAllocatableActionHandler.isChecked()) {
				// Undo booking (only available if checkbox is checked)
				menu.add(Menu.NONE, R.string.option_delete, 1,
						R.string.options_undo_booking);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == R.string.option_delete) {
			// Delete list item, show confirmation dialog before
			this.showDialog(DIALOG_CONFIRM_UNDO_BOOKING);
		}
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog dialog = null;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch (id) {
		case DIALOG_CONFIRM_UNDO_BOOKING:
			// Dialog to confirm that a booking for an allocatable should be
			// undone
			dialog = this.createDialogConfirmUndoBooking(builder);
			break;
		case DIALOG_ASSIGN_APPOINTMENTS:
			// Dialog for assigning appointments to an allocatable
			dialog = this.createDialogAssignAppointments(builder);
			break;
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_CONFIRM_UNDO_BOOKING:
			// Dialog to confirm that a booking for an allocatable should be
			// undone
			// Static dialog, no preparation necessary
			break;
		case DIALOG_ASSIGN_APPOINTMENTS:
			// Dialog for assigning appointments to an allocatable
			this.prepareDialogAssignAppointments(dialog);
			break;
		}
	}

	private void prepareDialogAssignAppointments(Dialog dialog) {
		// Get list view and delete all list items
		ListView list = ((AlertDialog) dialog).getListView();
		list.clearChoices();

		// Get all appointments of the selected reservation
		Appointment[] allAppointments = this.getSelectedReservation()
				.getAppointments();

		// Initialize array with appointments that are already assigned to the
		// selected allocatable
		Appointment[] assignedAppointments = new Appointment[0];

		// If the selected allocatable has already been assigned to the
		// reservation, get the appointments assigned to the allocatable.
		// Without
		// this condition, if an allocatable hasn't been assigned to the
		// reservation, getApointmentsFor(Allocatable) would return all
		// appointments
		if (this.getSelectedReservation().hasAllocated(
				this.selectedAllocatableActionHandler.currentAllocatable)) {
			assignedAppointments = this
					.getSelectedReservation()
					.getAppointmentsFor(
							this.selectedAllocatableActionHandler.currentAllocatable);
		}
		// Convert to array list for better code-readability and usage
		ArrayList<Appointment> assignedAppointmentsList = new ArrayList<Appointment>(
				Arrays.asList(assignedAppointments));

		// Loop at all appointments and check whether it has been assigned to
		// the allocatable
		for (int i = 0; i < allAppointments.length; i++) {
			Appointment appointment = allAppointments[i];

			if (assignedAppointmentsList.contains(appointment)) {
				list.setItemChecked(i, true);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Refresh list view
		this.refreshListView();
	}

	/**
	 * Initially create dialog for confirming undoing a booking
	 * 
	 * @param Alert
	 *            Dialog Builder to build the dialog
	 * @return Composed dialog
	 */
	public AlertDialog createDialogConfirmUndoBooking(
			AlertDialog.Builder builder) {
		DialogInterface.OnClickListener listener = new AllocatableUndoBookingDialogListener();
		builder.setMessage(R.string.allocatable_confirm_undo_booking)
				.setPositiveButton(R.string.yes, listener)
				.setNegativeButton(R.string.cancel, listener);
		return builder.create();
	}

	/**
	 * Initially create dialog for assigning appointments
	 * 
	 * @param Alert
	 *            Dialog Builder to build the dialog
	 * @return Composed dialog
	 */
	public AlertDialog createDialogAssignAppointments(
			AlertDialog.Builder builder) {

		Appointment[] appointments = this.getSelectedReservation()
				.getAppointments();

		CharSequence[] items = new CharSequence[appointments.length];
		for (int i = 0; i < appointments.length; i++) {
		    items[i] =  this.getAppointmentFormater().getSummary(appointments[i]);
		}

		builder.setTitle(R.string.allocatable_assign_appointments_dialog_title)
				.setPositiveButton(R.string.apply,
						new AllocatableAssignAppointmentsDialogListener())
				.setMultiChoiceItems(items, null, null);
		return builder.create();
	}

	/**
	 * Refresh list view by retrieving the latest data from the selected
	 * reservation
	 */
	public void refreshListView() {
        String stringExtra = this.getIntent().getStringExtra(   INTENT_STRING_ALLOCATABLE_CATEGORY_ELEMENT_KEY);
        ClientFacade facade = getFacade();
        try {
            DynamicType dynamicType;
            dynamicType = facade.getDynamicType( stringExtra);
            ClassificationFilter[] array = dynamicType.newClassificationFilter().toArray();
            Allocatable[] allocatables = facade.getAllocatables( array );
            AllocatableAdapter adapter = new AllocatableAdapter(this, this.getSelectedReservation(), allocatables);
            this.allocatableListView.setAdapter(adapter);
        } catch (RaplaException e) {
            e.printStackTrace();
        }
        
	}

	/**
	 * The list adapter cannot be kept as an instance attribute as for some
	 * reason, as soon as passing the reference to the async task for loading
	 * allocatables, the reference gets lost. So always retrieve it on the fly
	 * from the list view.
	 * 
	 * @return Casted list adapter of list view
	 */
	public AllocatableAdapter getListAdapter() {
		return (AllocatableAdapter) this.allocatableListView.getAdapter();
	}

	public AllocatableListItemCheckboxListener createAllocatableListItemCheckboxListener(
			int listItemIndex) {
		return new AllocatableListItemCheckboxListener(listItemIndex);
	}

	/**
	 * AllocatableUndoBookingDialogListener
	 * 
	 * This class handles the dialog started from the context menu for undoing
	 * booking an allocatable
	 * 
	 * @author Maximilian Lenkeit <dev@lenki.com>
	 * 
	 */
	public class AllocatableUndoBookingDialogListener implements
			DialogInterface.OnClickListener {

		public void onClick(DialogInterface dialog, int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				// yes, undo booking
				selectedAllocatableActionHandler.undoBooking();
				dialog.dismiss();
			} else {
				// Cancel
				dialog.dismiss();
			}
		}

	}

	/**
	 * SelectedAllocatableActionHandler
	 * 
	 * This class handles all actions related to an allocatable selected from
	 * the list view.
	 * 
	 * @author Maximilian Lenkeit <dev@lenki.com>
	 * 
	 */
	public class SelectedAllocatableActionHandler {

		private Allocatable currentAllocatable;
		private int listItemIndex;

		public void handleListItem(int i) {
			this.resetAttributes();
			this.listItemIndex = i;
			this.currentAllocatable = this
					.getAllocatableByListItemIndex(this.listItemIndex);
		}

		private void resetAttributes() {
			this.currentAllocatable = null;
			this.listItemIndex = -1;
		}

		private Allocatable getAllocatableByListItemIndex(int listItemIndex) {
			// return this.adapter.getItem(listItemIndex);
			return ((AllocatableAdapter) allocatableListView.getAdapter())
					.getItem(listItemIndex);
		}

		public void book() {
			getSelectedReservation().addAllocatable(this.currentAllocatable);
			// refreshListView();
		}

		public void undoBooking() {
			this.uncheckCheckbox();
			getSelectedReservation().removeAllocatable(this.currentAllocatable);
			refreshListView();
		}

		private void uncheckCheckbox() {
			CheckBox checkbox = this.getCheckbox();
			checkbox.setChecked(false);
			checkbox.setEnabled(true);
		}

		public boolean isChecked() {
			return this.getCheckbox().isChecked();
		}

		private CheckBox getCheckbox() {
			View listItemView = allocatableListView
					.getChildAt(this.listItemIndex);
			CheckBox checkbox = (CheckBox) listItemView
					.findViewById(R.id.allocatable_details_list_item_checkbox);
			return checkbox;
		}
	}

	/**
	 * AllocatableListItemCheckboxListener
	 * 
	 * This class handles the checkbox logic of a list item
	 * 
	 * @author Maximilian Lenkeit <dev@lenki.com>
	 * 
	 */
	public class AllocatableListItemCheckboxListener implements
			OnCheckedChangeListener {

		private int listItemIndex;

		public AllocatableListItemCheckboxListener(int listItemIndex) {
			this.listItemIndex = listItemIndex;
		}

		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {

			if (isChecked) {

				// Disable checkbox if being checked as unchecking it should
				// only be possible by using the context menu on the list item
				buttonView.setEnabled(false);

				// Book allocatable to reservation
				selectedAllocatableActionHandler
						.handleListItem(this.listItemIndex);
				selectedAllocatableActionHandler.book();

				// Go directly to dialog for assigning appointments to
				// allocatable
				showDialog(DIALOG_ASSIGN_APPOINTMENTS);
			}

		}

	}

	/**
	 * AllocatableListItemClickedListener
	 * 
	 * This class handles list item clicks. If the checkbox of the list item is
	 * checked, the dialog for assigning appointments to this allocatable is
	 * being started. Otherwise, no action is executed.
	 * 
	 * @author Maximilian Lenkeit <dev@lenki.com>
	 * 
	 */
	public class AllocatableListItemClickedListener implements
			OnItemClickListener {

		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {

			// Check whether checkbox of the clicked list item is checked
			CompoundButton checkBox = (CompoundButton) arg1
					.findViewById(R.id.allocatable_details_list_item_checkbox);
			if (checkBox.isChecked()) {

				// Initialize item handler
				selectedAllocatableActionHandler.handleListItem(position);

				// Show dialog for assigning appointments to the selected
				// allocatable
				showDialog(DIALOG_ASSIGN_APPOINTMENTS);
			}
		}

	}

	/**
	 * This class handles the dialog for assigning appointments to an
	 * allocatable. If no appointment is selected, the allocatable will be
	 * assigned to all appointments.
	 * 
	 * @author Maximilian Lenkeit <dev@lenki.com>
	 */
	public class AllocatableAssignAppointmentsDialogListener implements
			OnClickListener {

		public void onClick(DialogInterface dialog, int which) {
			// Get list view from dialog
			ListView list = ((AlertDialog) dialog).getListView();

			// Retrieve data necessary for subsequent calculations
			ReservationImpl reservation = getSelectedReservation();
			Appointment[] allAppointments = reservation.getAppointments();

			// Initialize array list for capturing restricted appointments from
			// the dialog and for appointments that the user cannot allocate
			ArrayList<Appointment> restrictedAppointments = new ArrayList<Appointment>();
			ArrayList<Appointment> rejectedAppointments = new ArrayList<Appointment>();

			// Loop at all appointments of the reservation and check whether the
			// current appointment is checked
			Allocatable currentAllocatable = selectedAllocatableActionHandler.currentAllocatable;
            for (int i = 0; i < allAppointments.length; i++) {
				if (list.isItemChecked(i)) {
					// If item is checked, check whether the user is allowed to
					// allocate the resource
					Appointment appointment = allAppointments[i];
                    Date today = getFacade().today();
                    User user;
                    try {
                        user = getFacade().getUser();
                    } catch (RaplaException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                    if (currentAllocatable.canAllocate(user, appointment.getStart(), appointment.getMaxEnd(),today))
                    {
						// User is allowed to allocate resource
						restrictedAppointments.add(appointment);
					} else {
						// User is not allowed to allocate resource
						rejectedAppointments.add(appointment);
						list.setItemChecked(i, false);
					}
				}
			}

			// Convert array list to native object array
			Appointment[] restrictedAppointmentArray = new Appointment[restrictedAppointments
					.size()];
			restrictedAppointments.toArray(restrictedAppointmentArray);

			// Restrict only if not all appointments were selected
			if (allAppointments.length > restrictedAppointmentArray.length) {
				reservation.setRestriction(
						currentAllocatable,
						restrictedAppointmentArray);
			} else {
				reservation.setRestriction(
						currentAllocatable,
						null);
			}

			// Check missing permission
			if (rejectedAppointments.size() == allAppointments.length) {
				Toast.makeText(AllocatableDetailsActivity.this,
						R.string.no_permission_to_allocate_all,
						Toast.LENGTH_LONG).show();
			} else if (rejectedAppointments.size() > 0) {
				Toast.makeText(
						AllocatableDetailsActivity.this,
						String.format(
								getString(R.string.no_persmission_to_allocate_for_x_of_y_appointments),
								rejectedAppointments.size(),
								allAppointments.length), Toast.LENGTH_LONG)
						.show();
			}

			// ... and refresh list view
			refreshListView();
		}
	}

}
