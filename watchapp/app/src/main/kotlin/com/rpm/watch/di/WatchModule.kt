package com.rpm.watch.di

import android.content.Context
import com.rpm.watch.data.WatchDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WatchModule {

    @Provides
    @Singleton
    fun provideWatchDataStore(
        @ApplicationContext context: Context
    ): WatchDataStore = WatchDataStore(context)
}
