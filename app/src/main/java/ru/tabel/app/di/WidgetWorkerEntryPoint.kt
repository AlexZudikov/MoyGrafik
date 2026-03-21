package ru.tabel.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.tabel.app.data.repository.TabelRepository
import ru.tabel.app.widget.WidgetUpdater

// Hilt EntryPoint для доступа к зависимостям из Worker
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetWorkerEntryPoint {
    fun repository(): TabelRepository
    fun widgetUpdater(): WidgetUpdater
}
