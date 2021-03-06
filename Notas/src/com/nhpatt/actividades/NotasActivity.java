package com.nhpatt.actividades;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.nhpatt.database.NotaDataBase;
import com.nhpatt.modelos.Nota;
import com.nhpatt.ws.ParseadorXML;
import com.nhpatt.ws.TraductorGoogle;

public class NotasActivity extends ListActivity {

	private static final int ACTIVIDAD_NUEVA_NOTA = 0;

	private SimpleCursorAdapter notaAdapter;
	private NotaDataBase dataBase;
	private Cursor cursor;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		dataBase = new NotaDataBase(this);
		dataBase.open();

		cursor = dataBase.findAll();
		startManagingCursor(cursor);

		notaAdapter = new SimpleCursorAdapter(this, R.layout.filanota, cursor,
				new String[] { NotaDataBase.DESCRIPCION, NotaDataBase.FECHA },
				new int[] { R.id.topText, R.id.bottomText });
		setListAdapter(notaAdapter);

		final ListView lista = (ListView) findViewById(android.R.id.list);
		registerForContextMenu(lista);

	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		super.onCreateOptionsMenu(menu);
		final MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menuprincipal, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuConocerMas:
			final Intent intentBrowser = new Intent(this, Browser.class);
			startActivity(intentBrowser);
			return true;
		case R.id.menuPreferencias:
			final Intent actividadPreferencias = new Intent(this,
					PreferenciasActivity.class);
			startActivity(actividadPreferencias);
			return true;
		case R.id.menuNuevaNota:
			final Intent actividadNuevaNota = new Intent(this,
					InsertarNotaActivity.class);
			startActivityForResult(actividadNuevaNota, ACTIVIDAD_NUEVA_NOTA);
			return true;
		case R.id.menuProcesarBlog:
			new ParseadorXML(this).execute();
			return true;
		case R.id.menuSalir:
			finish();
			return true;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v,
			final ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.contextmenu, menu);

		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		if (!preferences.getBoolean("PREF_TRADUCTOR_ACTIVADO", true)) {
			menu.findItem(R.id.traducirNota).setVisible(false);
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.eliminarNota:
			eliminarNota(item);
			return true;
		case R.id.traducirNota:
			traducirNota(item);
			return true;
		}
		return false;
	}

	private void eliminarNota(final MenuItem item) {
		final Nota nota = recuperarNotaDeLaLista(item);
		dataBase.eliminarNota(nota.getId());
		cursor.requery();
	}

	private void traducirNota(final MenuItem item) {
		final SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		final String descripcionTraducida = TraductorGoogle.traducir(
				recuperarNotaDeLaLista(item).getDescripcion(), "ES",
				preferences.getString("PREF_TRADUCTOR_IDIOMAS", "EN"));
		notificar(descripcionTraducida);
	}

	private void notificar(final String descripcionTraducida) {
		final int icon = R.drawable.icon;
		final String tickerText = "Nueva Nota traducida!";
		final long when = System.currentTimeMillis();
		final Notification notification = new Notification(icon, tickerText,
				when);

		final Intent intent = new Intent(this, NotasActivity.class);
		final PendingIntent launchIntent = PendingIntent.getActivity(
				getApplicationContext(), 0, intent, 0);

		notification.flags = notification.flags | Notification.DEFAULT_VIBRATE;

		notification.setLatestEventInfo(getApplicationContext(),
				"Nueva Nota traducida!", descripcionTraducida, launchIntent);

		final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(1, notification);

	}

	private Nota recuperarNotaDeLaLista(final MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		final Nota nota = dataBase.crearNotaDeCursor((Cursor) getListAdapter()
				.getItem(info.position));
		return nota;
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case ACTIVIDAD_NUEVA_NOTA:
			if (Activity.RESULT_OK == resultCode) {
				final Nota nota = (Nota) data
						.getSerializableExtra(InsertarNotaActivity.NOTA);
				dataBase.insertar(nota);
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected void onListItemClick(final ListView l, final View v,
			final int position, final long id) {
		super.onListItemClick(l, v, position, id);

		final Nota nota = dataBase.crearNotaDeCursor((Cursor) getListAdapter()
				.getItem(position));

		final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
		alertDialog.setCancelable(true);
		alertDialog.setTitle("Nota seleccionada");

		alertDialog.setMessage(nota.getDescripcion());
		alertDialog.setNeutralButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int which) {
					}
				});
		alertDialog.show();
	}

	@Override
	protected void onDestroy() {
		dataBase.close();
		super.onDestroy();
	}

	public void insertarResultado(final List<String> titulos) {
		for (final String titulo : titulos) {
			dataBase.insertar(new Nota(titulo));
		}
		cursor.requery();
	}
}