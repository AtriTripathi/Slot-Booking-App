package com.atritripathi.slotbooking;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
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
    private int mYear, mMonth, mDay, mHour, mMinute;
    private TextView appointmentDetails;
    private long calendarId, eventId;
    private int eventDate, eventMonth, eventYear, eventColor;
    private int beginHour, beginMinute, endHour, endMinute;
    private final int START_TIME = 0, END_TIME = 1;
    private final int PERMISSIONS_REQUEST_READ_CALENDAR = 1;
    private final int PERMISSION_REQUEST_CALENDAR_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {
                android.Manifest.permission.READ_CALENDAR,
                android.Manifest.permission.WRITE_CALENDAR
        };

        if(!hasPermissions(this, permissions)){
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CALENDAR_ALL);
        }


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        if (!prefs.getBoolean("firstTime", false)) {
            createCalendar();    // To create the calendar only once
        }

        editor.putBoolean("firstTime", true);   // Mark that the calendar has been created
        editor.apply();

        calendarId = getCalendarId();

        startTime = findViewById(R.id.et_start_time);
        endTime = findViewById(R.id.et_end_time);
        appointmentDate = findViewById(R.id.et_apnt_date);
        bookAppointmentButton = findViewById(R.id.button_book_appointment);
        deleteAppointmentButton = findViewById(R.id.button_delete_appointment);
        appointmentDetails = findViewById(R.id.tv_event_details);

        // Get the previously created Calendar Id

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
                addEvent(v);
                Toast.makeText(MainActivity.this, "Appointment Slot Booked", Toast.LENGTH_SHORT).show();
                getDataFromEventTable(v);
            }
        });

        deleteAppointmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteEvent(v);
                appointmentDetails.setText("");
//                deleteCalendar();   No need to delete the calendar specifically, I used it only as a helper method to delete the calendar created.
                Toast.makeText(MainActivity.this, "Booked Slot Deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }



    public void setDate(final EditText dateEditText) {
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        dateEditText.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                        eventDate = dayOfMonth;
                        eventMonth = monthOfYear;
                        eventYear = year;
                    }
                }, mYear, mMonth, mDay);
        datePickerDialog.show();
    }


    public void setTime(final EditText timeEditText, final int time) {
        final Calendar c = Calendar.getInstance();
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        timeEditText.setText(hourOfDay + ":" + minute);
                        if (time == START_TIME) {
                            beginHour = hourOfDay;
                            beginMinute = minute;
                        } else if (time == END_TIME) {
                            endHour = hourOfDay;
                            endMinute = minute;
                        }
                    }
                }, mHour, mMinute, false);
        timePickerDialog.show();
    }


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

    public void addEvent(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSIONS_REQUEST_READ_CALENDAR);
        }
        Calendar beginTime = Calendar.getInstance();
        beginTime.set(eventYear, eventMonth, eventDate, beginHour, beginMinute);

        Calendar endTime = Calendar.getInstance();
        endTime.set(eventYear, eventMonth, eventDate, endHour, endMinute);

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
    }


    public void deleteEvent(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSIONS_REQUEST_READ_CALENDAR);
        }
        Uri uri = CalendarContract.Events.CONTENT_URI;

        String mSelectionClause = CalendarContract.Events.TITLE + " = ?";
        String[] mSelectionArgs = {"Doctor's Appointment"};

        getContentResolver().delete(uri, mSelectionClause, mSelectionArgs);

    }

    public void getDataFromEventTable(View v) {
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


    public void createCalendar() {
        Log.d(TAG, "createCalendar: Calendar successfully created");
        Toast.makeText(MainActivity.this, "Calendar successfully created", Toast.LENGTH_SHORT).show();

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
    }

    private long getCalendarId() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR},
                    PERMISSIONS_REQUEST_READ_CALENDAR);
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


    private void deleteCalendar() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, PERMISSIONS_REQUEST_READ_CALENDAR);
        }

        Uri.Builder builder = CalendarContract.Calendars.CONTENT_URI.buildUpon();
        builder.appendPath(Long.toString(calendarId))   // here for testing; I know the calender has this ID
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "Dummy Account")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true");

        Uri uri = builder.build();
        getContentResolver().delete(uri, null, null);
    }

}
