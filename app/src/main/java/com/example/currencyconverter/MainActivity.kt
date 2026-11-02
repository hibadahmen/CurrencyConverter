package com.example.currencyconverter

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.currencyconverter.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Permet d'accéder aux vues du fichier activity_main.xml.
    private lateinit var binding: ActivityMainBinding

    // Taux fixes fournis dans le TP : valeurs pour 1 EUR.
    private val taux: Map<String, Double> = mapOf(
        "EUR" to 1.0,
        "USD" to 1.17,
        "TND" to 3.40,
        "GBP" to 0.87,
        "JPY" to 172.50,
        "CAD" to 1.62
    )

    private val longueurMaxMontant = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Création du binding et affichage de l'interface.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Évite que le contenu passe sous les barres du téléphone.
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { vue, insets ->
            val barres = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vue.setPadding(
                barres.left,
                barres.top,
                barres.right,
                barres.bottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(binding.main)

        initialiserDevises()
        initialiserClavier()
        initialiserActions()

        // Restaure les données si Android recrée l'activité.
        if (savedInstanceState != null) {
            restaurerEtat(savedInstanceState)
        }
    }

    private fun initialiserDevises() {
        // Les devises viennent du tableau déclaré dans strings.xml.
        val adaptateur = ArrayAdapter.createFromResource(
            this,
            R.array.devises,
            android.R.layout.simple_spinner_item
        )

        adaptateur.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        binding.spDeviseSource.adapter = adaptateur
        binding.spDeviseCible.adapter = adaptateur

        // Valeurs par défaut : USD vers EUR.
        binding.spDeviseSource.setSelection(
            adaptateur.getPosition("USD")
        )

        binding.spDeviseCible.setSelection(
            adaptateur.getPosition("EUR")
        )
    }

    private fun initialiserClavier() {
        // Toutes les touches numériques et le point utilisent
        // la même fonction de saisie.
        val touches = listOf(
            binding.btn0,
            binding.btn1,
            binding.btn2,
            binding.btn3,
            binding.btn4,
            binding.btn5,
            binding.btn6,
            binding.btn7,
            binding.btn8,
            binding.btn9,
            binding.btnPoint
        )

        for (touche in touches) {
            touche.setOnClickListener { vue ->
                val caractere = (vue as Button).text.toString()
                ajouterCaractere(caractere)
            }
        }

        binding.btnClear.setOnClickListener {
            binding.tvMontant.setText(R.string.initial_amount)
            binding.tvResultat.setText(R.string.empty_result)
        }
    }

    private fun initialiserActions() {
        binding.btnConvertir.setOnClickListener {
            afficherConversion()
        }

        binding.btnInverser.setOnClickListener {
            inverserDevises()
        }
    }

    private fun ajouterCaractere(c: String) {
        val montantActuel = binding.tvMontant.text.toString()

        // Un deuxième point décimal est ignoré.
        if (c == "." && montantActuel.contains(".")) {
            return
        }

        val nouveauMontant = when {
            // Permet de saisir un montant comme 0.5.
            montantActuel == "0" && c == "." -> "0."

            // Le premier chiffre remplace le zéro initial.
            montantActuel == "0" -> c

            // Sinon, le caractère est ajouté à la fin.
            else -> montantActuel + c
        }

        // Le montant ne doit pas dépasser 10 caractères.
        if (nouveauMontant.length > longueurMaxMontant) {
            return
        }

        binding.tvMontant.text = nouveauMontant

        // Le précédent résultat ne correspond plus au montant.
        binding.tvResultat.setText(R.string.empty_result)
    }

    private fun convertir(
        montant: Double,
        source: String,
        cible: String
    ): Double {
        // L'euro sert de devise pivot.
        val montantEnEuros = montant / taux.getValue(source)
        return montantEnEuros * taux.getValue(cible)
    }

    private fun afficherConversion() {
        val texteMontant = binding.tvMontant.text.toString()
        val montant = texteMontant.toDoubleOrNull()

        if (montant == null || !montant.isFinite()) {
            binding.tvResultat.setText(R.string.empty_result)

            Toast.makeText(
                this,
                R.string.invalid_amount,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (montant == 0.0) {
            binding.tvResultat.setText(R.string.empty_result)

            Toast.makeText(
                this,
                R.string.amount_required,
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val deviseSource =
            binding.spDeviseSource.selectedItem.toString()

        val deviseCible =
            binding.spDeviseCible.selectedItem.toString()

        val resultat = convertir(
            montant,
            deviseSource,
            deviseCible
        )

        // Locale.US garde le point décimal comme dans le sujet.
        binding.tvResultat.text = String.format(
            Locale.US,
            "%.2f",
            resultat
        )
    }

    private fun inverserDevises() {
        // On mémorise l'état avant d'échanger les sélections.
        val resultatDejaAffiche =
            binding.tvResultat.text.isNotEmpty()

        val positionSource =
            binding.spDeviseSource.selectedItemPosition

        val positionCible =
            binding.spDeviseCible.selectedItemPosition

        binding.spDeviseSource.setSelection(positionCible)
        binding.spDeviseCible.setSelection(positionSource)

        // Le montant reste identique ; seules les devises changent.
        if (resultatDejaAffiche) {
            afficherConversion()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(
            "montant",
            binding.tvMontant.text.toString()
        )

        outState.putString(
            "resultat",
            binding.tvResultat.text.toString()
        )

        outState.putInt(
            "source",
            binding.spDeviseSource.selectedItemPosition
        )

        outState.putInt(
            "cible",
            binding.spDeviseCible.selectedItemPosition
        )

        super.onSaveInstanceState(outState)
    }

    private fun restaurerEtat(etat: Bundle) {
        binding.tvMontant.text = etat.getString(
            "montant",
            getString(R.string.initial_amount)
        )

        binding.tvResultat.text = etat.getString(
            "resultat",
            getString(R.string.empty_result)
        )

        binding.spDeviseSource.setSelection(
            etat.getInt("source", 0)
        )

        binding.spDeviseCible.setSelection(
            etat.getInt("cible", 1)
        )
    }
}