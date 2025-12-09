package com.eduquiz.app.navigation

enum class RootDestination(val route: String, val title: String) {
    Home("home", "Inicio"),
    Auth("auth", "Autenticación"),
    Pack("pack", "Packs"),
    Exam("exam", "Exámenes"),
    Profile("profile", "Perfil"),
    Store("store", "Tienda"),
    Ranking("ranking", "Tabla de clasificación"),
    Settings("settings", "Ajustes");

    companion object {
        val allDestinations = values().toList()
    }
}
