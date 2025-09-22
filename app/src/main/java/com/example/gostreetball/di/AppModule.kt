package com.example.gostreetball.di

import android.content.Context
import android.location.Geocoder
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.gostreetball.data.repo.AuthRepository
import com.example.gostreetball.data.repo.CourtRepository
import com.example.gostreetball.data.repo.GameRepository
import com.example.gostreetball.data.repo.UserRepository
import com.example.gostreetball.location.LocationRepository
import com.example.gostreetball.utils.EloSystem
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    fun provideFirebaseFireStore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideGeocoder(@ApplicationContext context: Context): Geocoder {
        return Geocoder(context, Locale.getDefault())
    }

    @Provides
    @Singleton
    fun provideEloSystem(): EloSystem = EloSystem()

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepository(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firebaseFireStore: FirebaseFirestore,
        firebaseStorage: FirebaseStorage
    ): AuthRepository {
        return AuthRepository(firebaseAuth, firebaseFireStore, firebaseStorage)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        firebaseAuth: FirebaseAuth,
        firebaseFireStore: FirebaseFirestore,
        firebaseStorage: FirebaseStorage
    ): UserRepository {
        return UserRepository(firebaseAuth, firebaseFireStore, firebaseStorage)
    }

    @Provides
    @Singleton
    fun provideCourtRepository(
        firebaseAuth: FirebaseAuth,
        firebaseFireStore: FirebaseFirestore,
        firebaseStorage: FirebaseStorage,
        geocoder: Geocoder
    ): CourtRepository {
        return CourtRepository(firebaseAuth, firebaseFireStore, firebaseStorage, geocoder)
    }

    @Provides
    @Singleton
    fun provideGameRepository(
        firebaseAuth: FirebaseAuth,
        firebaseFireStore: FirebaseFirestore,
        eloSystem: EloSystem
    ): GameRepository {
        return GameRepository(firebaseFireStore, firebaseAuth, eloSystem)
    }

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings") }
        )
    }
}