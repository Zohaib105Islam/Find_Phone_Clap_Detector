package com.base.find_phone_clap_detector.module

import android.app.Application
import android.content.Context
import com.base.find_phone_clap_detector.ui.repository.AudioRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.net.ssl.*

@Module
@InstallIn(SingletonComponent::class)
class MyModule {

    var context: Context? = null
    @Singleton
    @Provides
    fun provideContext(application: Application): Context {
        context = application.applicationContext

        return context!!
    }

    @Provides
    @Singleton
    fun provideAudioRepository(@ApplicationContext context: Context): AudioRepository {
        return AudioRepository(context)
    }
}
