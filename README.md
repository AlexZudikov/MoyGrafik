<div align="center">
<img width="1024" height="500" alt="ic_launcher_foreground" src="https://github.com/user-attachments/assets/943a3990-9a33-4db2-95d6-b140bc39ab51" />

# 📅 Мой График

**Приложение для учёта рабочих смен на Android**

[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-v8-FF6F00)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-1.0-orange)]()

[**Возможности**](#-возможности) · [**Скриншоты**](#-скриншоты) · [**Установка**](#-установка) · [**Архитектура**](#-архитектура)

</div>

---

## 🌟 О приложении

**Мой График** — нативное Android-приложение для работников со сменным графиком. Веди расписание смен, получай уведомления, отслеживай статистику и экспортируй данные в PDF.

> Полностью написано на **Kotlin + Jetpack Compose** с современным Android-стеком и чистой архитектурой MVVM.

---

## ✨ Возможности

### 📅 Календарь
- Месячная сетка смен с цветовой кодировкой по типам
- Автозаполнение по шаблонам: 2/2, 3/3, 5/2, 6/1, 14/14 и другие
- Карточка ближайшей смены с датой и обратным отсчётом
- Выделение сегодняшней даты с анимацией пульсации и свечением

### 📊 Статистика
- Подсчёт рабочих, ночных смен, выходных и больничных
- Расчёт зарплаты с ночным и праздничным коэффициентами
- Учёт обедов и перерывов при подсчёте рабочих часов
- Норма часов, переработки, история за 6 месяцев
- Экспорт в PDF с выбором периода

### 👤 Профили
- Несколько независимых профилей (для разных работ / членов семьи)
- Замок от случайного удаления 🔒
- Переименование профилей ✏️
- Переключение одним касанием

### 🔔 Уведомления
- Напоминание за N часов до начала смены
- Собственный звук уведомления
- Автовосстановление после перезагрузки телефона
- Напоминание заполнить следующий месяц (25-го числа)

### 🧩 Виджет рабочего стола
- Три размера: Small (2×1), Medium (2×2), Large (4×2)
- Показывает дату, день недели, ближайшие смены
- Автообновление каждый час через WorkManager
- Время работы только для рабочих смен

### 🎨 UX / Анимации
- Заставка в стиле Telegram (расходящиеся круги)
- 12 анимационных утилит
- Плавные переходы 350 мс
- Градиентные кнопки, анимация замка, пульс сегодняшней даты

---

## 📸 Скриншоты

<div align="center">

| Календарь | Смены | Статистика | Настройки |
|:---------:|:-----:|:----------:|:---------:|
| <img src="screenshots/calendar.png" width="200"/> | <img src="screenshots/shifts.png" width="200"/> | <img src="screenshots/stats.png" width="200"/> | <img src="screenshots/settings.png" width="200"/> |

</div>
---

## 🛠 Стек технологий

| Категория | Технология | Версия |
|-----------|-----------|--------|
| **UI** | Jetpack Compose + Material 3 | BOM 2024.02 |
| **Язык** | Kotlin | 1.9.22 |
| **Архитектура** | MVVM + Repository + Hilt | 2.50 |
| **База данных** | Room + SQLite | 2.6.1 |
| **Навигация** | Navigation Compose | 2.7.6 |
| **Фоновые задачи** | WorkManager + AlarmManager | 2.9.0 |
| **Виджет** | Glance AppWidget | 1.1.0 |
| **Безопасность** | EncryptedSharedPreferences | 1.0.0 |
| **DI** | Hilt | 2.50 |
| **Min SDK** | Android 8.0 | API 26 |
| **Target SDK** | Android 14 | API 34 |

---

## 📁 Архитектура

```
app/src/main/java/ru/tabel/app/
│
├── MainActivity.kt                 # Точка входа, Bottom Navigation
├── TabelApplication.kt             # Hilt, WorkManager, инициализация
├── SettingsViewModel.kt            # Глобальный ViewModel настроек
│
├── data/
│   ├── db/
│   │   ├── TabelDatabase.kt        # Room БД (version 8), миграции 1→8
│   │   └── Daos.kt                 # ShiftDao, ProfileDao, SettingsDao
│   ├── model/
│   │   └── Models.kt               # ShiftEntry, Profile, AppSettings
│   └── repository/
│       └── TabelRepository.kt      # Единая точка доступа к данным
│
├── di/
│   ├── AppModule.kt                # Hilt модуль
│   └── WidgetWorkerEntryPoint.kt   # EntryPoint для Worker
│
├── notifications/
│   ├── TabelNotificationManager.kt # AlarmManager, каналы, звуки
│   ├── NotificationReceiver.kt     # BroadcastReceiver
│   └── BootReceiver.kt             # Восстановление после перезагрузки
│
├── ui/
│   ├── calendar/                   # Главный экран + автозаполнение
│   ├── shifts/                     # Список смен с фильтрами
│   ├── stats/                      # Статистика + PDF экспорт
│   ├── settings/                   # Настройки приложения
│   ├── profile/                    # Управление профилями
│   ├── splash/                     # Заставка при запуске
│   ├── onboarding/                 # Экран онбординга
│   ├── components/                 # Переиспользуемые компоненты
│   └── theme/                      # Тема + типография + анимации
│
└── widget/
    ├── ShiftWidget.kt              # Glance виджет (3 размера)
    ├── WidgetUpdater.kt            # Обновление данных виджета
    └── WidgetUpdateWorker.kt       # WorkManager worker
```

### Схема архитектуры

```
┌─────────────────────────────────────┐
│              UI Layer               │
│  Compose Screens + ViewModels       │
└──────────────┬──────────────────────┘
               │ StateFlow / collectAsState
┌──────────────▼──────────────────────┐
│           Domain Layer              │
│         TabelRepository             │
└──────┬───────────────────┬──────────┘
       │                   │
┌──────▼──────┐    ┌───────▼──────────┐
│  Room DB    │    │ SharedPreferences │
│  (SQLite)   │    │  (виджет, замки)  │
└─────────────┘    └──────────────────┘
```

---

## 🎨 Типы смен

| Тип | Цвет | Код |
|-----|------|-----|
| ☀️ Дневная | 🔵 | `#2563EB` |
| 🌙 Ночная | 🟣 | `#7C3AED` |
| 😴 Отсыпной | 🟣 | `#9333EA` |
| 🏠 Выходной | 🟢 | `#16A34A` |
| 🎁 Праздник | 🔴 | `#DC2626` |
| 🤒 Больничный | 🟠 | `#EA580C` |
| 🌴 Отпуск | 🔵 | `#0891B2` |

---

## 🚀 Установка

### Требования
- **Android Studio** Hedgehog 2023.1.1 или новее
- **JDK** 17
- **Android SDK** 34

### Сборка из исходников

```bash
# 1. Клонировать репозиторий
git clone https://github.com/YOUR_USERNAME/moygrafik.git
cd moygrafik

# 2. Собрать debug APK
./gradlew assembleDebug

# 3. Установить на подключённое устройство
./gradlew installDebug
```

### Ключевые зависимости

```gradle
// BOM для Compose
implementation platform('androidx.compose:compose-bom:2024.02.00')

// Dependency Injection
implementation 'com.google.dagger:hilt-android:2.50'

// База данных
implementation "androidx.room:room-runtime:2.6.1"
implementation "androidx.room:room-ktx:2.6.1"

// Виджет
implementation 'androidx.glance:glance-appwidget:1.1.0'
implementation 'androidx.glance:glance-material3:1.1.0'

// Фоновые задачи
implementation 'androidx.work:work-runtime-ktx:2.9.0'

// Безопасное хранение
implementation 'androidx.security:security-crypto:1.0.0'
```

---

## 🗄 История базы данных

| Версия | Изменение |
|--------|-----------|
| 1 → 2 | Добавлены themeMode, cloudBackup |
| 2 → 3 | No-op (совместимость) |
| 3 → 4 | Добавлен sickCoeff |
| 4 → 5 | Добавлен notifDayTime |
| 5 → 6 | Пересоздание таблицы settings |
| 6 → 7 | Финальная санация схемы |
| 7 → 8 | Добавлен breakMinutes (перерывы) |

---

## 📄 Лицензия

```
MIT License — Copyright (c) 2026 Alexandr Zudikov

Разрешается свободное использование, копирование и изменение
при сохранении оригинального авторства.
```

---
<div align="center">

Разработчик: **Alexander Zudikov (Александр Зудиков)**

---

*Если приложение полезно — поставь ⭐ звезду на GitHub!*

</div>
