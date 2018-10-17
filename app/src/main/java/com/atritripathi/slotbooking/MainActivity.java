package com.atritripathi.slotbooking;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Button bookAppointmentButton, deleteAppointmentButton;
    private EditText startTime, endTime, appointmentDate;
    private TextView appointmentDetails;
    private long calendarId, eventId;
    private int eventDate, eventMonth, eventYear, eventColor;
    private int beginHour, beginMinute, endHour, endMinute;
    private final int START_TIME = 0, END_TIME = 1;
    private final int PERMISSIONS_REQUEST_READ_CALENDAR = 2;
    private final int PERMISSION_REQUEST_CALENDAR_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23)
            checkPermission();
        else
            initialiseViews();
    }


    /**
     * Used to validate whether specified permissions are granted by the user or not, and accordingly
     * request the user to allow the requested permisssions
     */
    private void checkPermission() {
        String[] permissions = {
                android.Manifest.permission.READ_CALENDAR,
                android.Manifest.permission.WRITE_CALENDAR
        };

        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CALENDAR_ALL);
        } else
            initialiseViews();
    }


    /**
     * Helper method to check for granted permissions
     *
     * @param context     is the activity context for which the permissions are being asked
     * @param permissions is a list of permissions to be checked whether they are allowed or not.
     * @return true if all permissions have been granted, else false
     */
    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Used to initialise all the views and assign values to the various fields, for the current activity
     */
    public void initialiseViews() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        if (!prefs.getBoolean("firstTime", false)) {
            if (!isCalendarCreated())
                createCalendar();       // To create the calendar only once
        }

        editor.putBoolean("firstTime", true);   // Mark that the calendar has been created
        editor.apply();

        // Get the ID of the Calendar created above.
        calendarId = getCalendarId();

        startTime = findViewById(R.id.et_start_time);
        endTime = findViewById(R.id.et_end_time);
        appointmentDate = findViewById(R.id.et_apnt_date);
        bookAppointmentButton = findViewById(R.id.button_book_appointment);
        deleteAppointmentButton = findViewById(R.id.button_delete_appointment);
        appointmentDetails = findViewById(R.id.tv_event_details);

        appointmentDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDate(appointmentDate);
            }
        });

        startTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTime(startTime, START_TIME);
            }
        });

        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTime(endTime, END_TIME);
            }
        });

        bookAppointmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If Calendar is not created yet, then create one.
//                if (getCalendarId() == -1) {
//                    createCalendar();
//                }
                if (!isCalendarCreated())
                    createCalendar();

                addEvent(v);
                Toast.makeText(MainActivity.this, "Appointment Slot Booked", Toast.LENGTH_SHORT).show();
                // Show the data in the TextView
                getDataFromEventTable(v);
            }
        });

        deleteAppointmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteEvent(v);
                appointmentDetails.setText("");
                Toast.makeText(MainActivity.this, "Booked Slot Deleted", Toast.LENGTH_SHORT).show();

//                No need to delete the calendar specifically,
//                I used it only as a helper method to delete the calendar already by us.
//                if (getCalendarId() != -1)
//                    deleteCalendar();
            }
        });
    }


    /**
     * Callback method, which is used to again ask for permission when the request has been declined by the user
     *
     * @param requestCode  to validate the proper flow permission request
     * @param permissions  is the set of permissions to be requested by the user.
     * @param grantResults to check whether request was accepted or declined by the user, initially.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initialiseViews();
                } else {
                    //permission is denied (this is the first time, when "never ask again" is not checked)
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                        finish();
                    }
                    //permission is denied (and never ask again is  checked)
                    else {
                        //shows the dialog describing the importance of permission, so that user should grant
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage("You have forcefully denied Calendar access permission.\n\n"
                                + "This is necessary for the working of app." + "\n\n"
                                + "Click on 'Accept' to grant permission")
                                //This will open app information where user can manually grant requested permission
                                .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                    }
                                })
                                //close the app
                                .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });
                        builder.setCancelable(false);
                        builder.create().show();
                    }
                }
        }
    }


    /**
     * Used to set the Date, selected from the DatePickerDialog
     *
     * @param dateEditText view on which this method should respond on click event
     */
    public void setDate(final EditText dateEditText) {
        final Calendar c = Calendar.getInstance();
        int mYear = c.get(Calendar.YEAR);
        int mMonth = c.get(Calendar.MONTH);
        int mDay = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        String date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                        dateEditText.setText(date);
                        eventDate = dayOfMonth;
                        eventMonth = monthOfYear;
                        eventYear = year;
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }


    /**
     * Used to set the Time, selected from the TimePickerDialog
     *
     * @param timeEditText view on which this method should respond on click event
     * @param TIME         is constant used to differentiate between start/end time
     */
    public void setTime(final EditText timeEditText, final int TIME) {
        final Calendar c = Calendar.getInstance();
        int mHour = c.get(Calendar.HOUR_OF_DAY);
        int mMinute = c.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        timeEditText.setText(hourOfDay + ":" + minute);
                        if (TIME == START_TIME) {
                            beginHour = hourOfDay;
                            beginMinute = minute;
                        } else if (TIME == END_TIME) {
                            endHour = hourOfDay;
                            endMinute = minute;
                        }
                    }
                }, mHour, mMinute, false);
        timePickerDialog.show();
    }


    /**
     * Used to select an Appointment reason from the radio buttons
     *
     * @param view on which this method responds, during any click event.
     */
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radio_follow_up:
                if (checked)
                    eventColor = Color.GREEN;
                break;
            case R.id.radio_sick_visit:
                if (checked)
                    eventColor = Color.RED;
                break;
            case R.id.radio_vaccination:
                if (checked)
                    eventColor = Color.YELLOW;
                break;
        }
    }


    private boolean isCalendarCreated() {
        long calendarId = getCalendarId();
        if (calendarId != -1)
            return true;
        else
            return false;
    }


    /**
     * Used to create a new local Calendar with values, as specified
     */
    public void createCalendar() {
        Log.d(TAG, "createCalendar: Calendar successfully created");

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, "Dummy Account");
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(CalendarContract.Calendars.NAME, "Appointments Calendar");
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Appointments Calendar");
        values.put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFF03A9F4);
        values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        values.put(CalendarContract.Calendars.OWNER_ACCOUNT, "somebody@gmail.com");
        values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, "Asia/Kolkata");
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);

        Uri.Builder builder = CalendarContract.Calendars.CONTENT_URI.buildUpon();
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "Dummy Account");
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");
        Uri uri = getContentResolver().insert(builder.build(), values);

        Toast.makeText(MainActivity.this, "Calendar successfully created", Toast.LENGTH_SHORT).show();
    }


    /**
     * Used to find the ID of the local Calendar created by us in the system's Calendar Table
     *
     * @return the Calendar ID(long) if it is found, else return -1
     */
    private long getCalendarId() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, PERMISSIONS_REQUEST_READ_CALENDAR);
        }

        String[] projection = new String[]{CalendarContract.Calendars._ID};
        String selection = CalendarContract.Calendars.ACCOUNT_NAME + " = ? AND " + CalendarContract.Calendars.ACCOUNT_TYPE + " = ? ";
        String[] selArgs = new String[]{"Dummy Account", CalendarContract.ACCOUNT_TYPE_LOCAL};

        Cursor cursor = getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, projection, selection, selArgs, null);

        if (cursor.moveToFirst()) {
            return cursor.getLong(0);
        }
        return -1;
    }


    /**
     * Used to add a new Event in the system's Events Table
     *
     * @param view on which this method responds, during click event (eg: "Book Appointment" button)
     */
    public void addEvent(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSIONS_REQUEST_READ_CALENDAR);
        }
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(eventYear, eventMonth, eventDate, beginHour, beginMinute);

        Calendar endTime = Calendar.getInstance();
        endTime.set(eventYear, eventMonth, eventDate, endHour, endMinute);

//        if (checkEventClash(beginTime.getTimeInMillis(), endTime.getTimeInMillis())) {
//            Toast.makeText(MainActivity.this, "Slot already booked", Toast.LENGTH_SHORT).show();
//        } else {

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
        values.put(CalendarContract.Events.DTSTART, beginTime.getTimeInMillis());
        values.put(CalendarContract.Events.DTEND, endTime.getTimeInMillis());
        values.put(CalendarContract.Events.TITLE, "Doctor's Appointment");
        values.put(CalendarContract.Events.DESCRIPTION, "Dummy data which can be taken from the user's input in a text field, later on. ");
        values.put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Kolkata");
        values.put(CalendarContract.Events.EVENT_LOCATION, "Bangalore");
        values.put(CalendarContract.Events.EVENT_COLOR, eventColor);
        values.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE);

        Uri uri = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);
        eventId = Long.valueOf(uri.getLastPathSegment());
//        }
    }


    /**
     * Used to delete an existing Event from the system's Events Table
     *
     * @param view on which this method responds, during click event (eg: "Delete Appointment" button)
     */
    public void deleteEvent(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSIONS_REQUEST_READ_CALENDAR);
        }
        Uri uri = CalendarContract.Events.CONTENT_URI;

        String mSelectionClause = CalendarContract.Events.TITLE + " = ?";
        String[] mSelectionArgs = {"Doctor's Appointment"};     // Right now, name is being used. Later, this can be changed to ID of each particular event

        getContentResolver().delete(uri, mSelectionClause, mSelectionArgs);
    }


    /**
     * Used to retrieve and display the Event's data from the system's Event Table
     *
     * @param view on which this method responds, during click event (eg: "Book Appointment" button)
     */
    public void getDataFromEventTable(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSIONS_REQUEST_READ_CALENDAR);
        }

        String[] mProjection =
                {
                        CalendarContract.Events._ID,
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.DESCRIPTION,
                        CalendarContract.Events.EVENT_LOCATION,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.DTEND,
                };

        Uri uri = CalendarContract.Events.CONTENT_URI;
        String selection = CalendarContract.Events._ID + " = ? ";
        String[] selectionArgs = new String[]{Long.toString(eventId)};

        Cursor cur = getContentResolver().query(uri, mProjection, selection, selectionArgs, null);

        while (cur.moveToNext()) {
            String details = cur.getString(cur.getColumnIndex(CalendarContract.Events.TITLE)) + "\n"
                    + cur.getString(cur.getColumnIndex(CalendarContract.Events.DESCRIPTION)) + "\n"
                    + cur.getString(cur.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)) + "\n";
            appointmentDetails.setText(details);
        }
        cur.close();
    }


    /**
     * ********************* EXPERIMENTAL CODE, BEGINS HERE ***********************
     */

    // Not needed generally. Only used for testing purpose
//    private void deleteCalendar() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, PERMISSIONS_REQUEST_READ_CALENDAR);
//        }
//
//        Uri.Builder builder = CalendarContract.Calendars.CONTENT_URI.buildUpon();
//        builder.appendPath(Long.toString(calendarId))   // here for testing; I know the calender has this ID
//                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "Dummy Account")
//                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
//                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");
//
//        Uri uri = builder.build();
//        getContentResolver().delete(uri, null, null);
//    }


//    private boolean checkEventClash(long startTime, long endTime) {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR},
//                    PERMISSIONS_REQUEST_READ_CALENDAR);
//        }
//
//        String[] proj = new String[]{
//                CalendarContract.Events._ID,
//                CalendarContract.Events.DTSTART,
//                CalendarContract.Events.DTEND};
////        Cursor cursor = CalendarContract.Instances.query(getContentResolver(), proj, startTime, endTime);
//
//        Uri uri = CalendarContract.Events.CONTENT_URI;
//        String selection = CalendarContract.Events._ID + " = ? AND "
//                + CalendarContract.Events.DTSTART + " = ?";
//        String[] selectionArgs = new String[]{Long.toString(eventId),Long.toString(startTime)};
//
//        Cursor cursor = getContentResolver().query(uri, proj, selection, selectionArgs, null);
//        if (cursor.getCount() > 0) {
//            return true;
//        }
//        return false;
//    }

    /**
     * *********************** EXPERIMENTAL CODE, ENDS HERE ***********************
     */

}
