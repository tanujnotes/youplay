package com.youplay;

import android.content.Context;
import android.util.Log;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.StandardDatabase;

/**
 * Created by tan on 08/05/17.
 **/

public class DbOpenHelper extends DaoMaster.OpenHelper {

    public DbOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        Log.i("greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion);
        MigrationHelper.migrate((StandardDatabase) db,
                FavoriteDaoModelDao.class,
                HistoryDaoModelDao.class);
    }
}