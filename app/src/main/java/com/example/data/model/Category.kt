package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

data class GroceryCategory(
    val id: String,
    val name: String,
    val englishName: String,
    val emoji: String,
    val description: String,
    val aisleNumber: Int,
    val color: Color
) {
    val displayLabel: String
        get() = "$emoji $name · $englishName"

    val shortLabel: String
        get() = "$emoji $name"
}

fun String.normalizeForSearch(): String {
    val temp = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(temp, "").lowercase().trim()
}

object Categories {
    val ALL = listOf(
        GroceryCategory(
            id = "hortifruti",
            name = "Hortifruti",
            englishName = "Produce",
            emoji = "🥦",
            description = "Frutas, verduras, legumes e temperos frescos (Fruits & Vegetables)",
            aisleNumber = 1,
            color = CatHortifruti
        ),
        GroceryCategory(
            id = "padaria",
            name = "Padaria & Confeitaria",
            englishName = "Bakery",
            emoji = "🥖",
            description = "Pães frescos, bolos, torradas e biscoitos artesanais",
            aisleNumber = 2,
            color = CatPadaria
        ),
        GroceryCategory(
            id = "laticinios",
            name = "Laticínios & Frios",
            englishName = "Dairy",
            emoji = "🥛",
            description = "Leites, queijos, iogurtes, ovos e manteigas",
            aisleNumber = 3,
            color = CatLaticinios
        ),
        GroceryCategory(
            id = "acougue",
            name = "Açougue & Peixaria",
            englishName = "Meat & Seafood",
            emoji = "🥩",
            description = "Carnes bovinas, frango, suíno e peixes frescos",
            aisleNumber = 4,
            color = CatAcougue
        ),
        GroceryCategory(
            id = "mercearia",
            name = "Mercearia & Grãos",
            englishName = "Pantry & Grains",
            emoji = "🍚",
            description = "Arroz, feijão, massas, óleos, azeites e temperos",
            aisleNumber = 5,
            color = CatMercearia
        ),
        GroceryCategory(
            id = "congelados",
            name = "Congelados & Prontos",
            englishName = "Frozen Foods",
            emoji = "❄️",
            description = "Pizzas, hambúrgueres, batatas e refeições rápidas",
            aisleNumber = 6,
            color = CatCongelados
        ),
        GroceryCategory(
            id = "bebidas",
            name = "Bebidas",
            englishName = "Beverages",
            emoji = "🥤",
            description = "Água, sucos naturais, cafés, chás, refrigerantes e cervejas",
            aisleNumber = 7,
            color = CatBebidas
        ),
        GroceryCategory(
            id = "limpeza",
            name = "Limpeza & Lavanderia",
            englishName = "Cleaning",
            emoji = "🧹",
            description = "Detergentes, sabão, amaciante e desinfetantes",
            aisleNumber = 8,
            color = CatLimpeza
        ),
        GroceryCategory(
            id = "higiene",
            name = "Higiene Pessoal",
            englishName = "Personal Care",
            emoji = "🧴",
            description = "Papel higiênico, sabonete, xampu, creme dental",
            aisleNumber = 9,
            color = CatHigiene
        ),
        GroceryCategory(
            id = "petiscos",
            name = "Petiscos & Doces",
            englishName = "Snacks & Sweets",
            emoji = "🍫",
            description = "Chocolates, biscoitos recheados e salgadinhos",
            aisleNumber = 10,
            color = CatPetiscos
        ),
        GroceryCategory(
            id = "outros",
            name = "Outros",
            englishName = "Other",
            emoji = "📦",
            description = "Utilidades gerais, bazar e diversos",
            aisleNumber = 11,
            color = CatOutros
        )
    )

    fun getByName(name: String): GroceryCategory {
        val clean = name.trim().lowercase()
        return ALL.firstOrNull { it.id.equals(clean, ignoreCase = true) }
            ?: ALL.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: ALL.firstOrNull { it.englishName.equals(clean, ignoreCase = true) }
            ?: when (clean) {
                "produce", "hortifruti", "frutas", "vegetables", "fruits", "legumes", "verduras" -> ALL[0]
                "bakery", "padaria", "bread", "breads", "paes", "confeitaria", "pastry" -> ALL[1]
                "dairy", "laticinios", "frios", "cheese", "milk", "leite", "queijos" -> ALL[2]
                "meat", "seafood", "acougue", "peixaria", "carnes", "butcher" -> ALL[3]
                "pantry", "grains", "mercearia", "graos", "grocery" -> ALL[4]
                "frozen", "congelados", "freezer" -> ALL[5]
                "beverages", "drinks", "bebidas", "suco", "refrigerante" -> ALL[6]
                "cleaning", "limpeza", "lavanderia", "household" -> ALL[7]
                "hygiene", "personal care", "higiene", "perfumaria" -> ALL[8]
                "snacks", "sweets", "petiscos", "doces", "bomboniere" -> ALL[9]
                else -> ALL.firstOrNull { it.name.normalizeForSearch().contains(clean.normalizeForSearch()) }
                    ?: ALL.firstOrNull { it.englishName.normalizeForSearch().contains(clean.normalizeForSearch()) }
                    ?: ALL.last() // Outros
            }
    }

    /**
     * Intelligent category detector with bilingual (Portuguese & English) NLP matching
     * and accent-insensitive normalization.
     */
    fun detectCategory(productName: String): GroceryCategory {
        val lower = productName.normalizeForSearch()
        val words = lower.split(Regex("[\\s,.-]+")).filter { it.isNotBlank() }.toSet()

        fun matches(keywords: List<String>): Boolean {
            return keywords.any { kw ->
                val normKw = kw.normalizeForSearch()
                if (normKw.contains(" ")) {
                    lower.contains(normKw)
                } else {
                    words.contains(normKw) || (normKw.length >= 4 && words.any { it.startsWith(normKw) })
                }
            }
        }

        // 1. Hortifruti / Produce (Frutas, verduras, legumes, temperos frescos)
        val hortifrutiKeywords = listOf(
            "produce", "fruit", "fruits", "vegetable", "vegetables", "herb", "herbs",
            "banana", "maca", "apple", "apples", "laranja", "orange", "oranges",
            "limao", "lemon", "lime", "tomate", "tomato", "tomatoes",
            "cebola", "onion", "onions", "alho", "garlic", "batata", "potato", "potatoes",
            "cenoura", "carrot", "carrots", "alface", "lettuce", "rucula", "arugula",
            "uva", "grape", "grapes", "morango", "strawberry", "strawberries",
            "melancia", "watermelon", "melao", "melon", "abacaxi", "pineapple",
            "manga", "mango", "mamao", "papaya", "abacate", "avocado",
            "pepino", "cucumber", "pimentao", "pepper", "bell pepper",
            "abobrinha", "zucchini", "berinjela", "eggplant", "mandioca", "cassava",
            "aipim", "macaxeira", "cheiro verde", "coentro", "cilantro", "salsinha", "parsley",
            "cebolinha", "manjericao", "basil", "couve", "kale", "brocolis", "broccoli",
            "espinafre", "spinach", "maracuja", "passion fruit", "pera", "pear", "pessego", "peach",
            "ameixa", "plum", "kiwi", "goiaba", "guava", "tangerina", "tangerine",
            "mexerica", "bergamota", "ponkan", "caju", "coco verde", "coconut", "coco seco",
            "beterraba", "beet", "chuchu", "vagem", "green beans", "quiabo", "okra",
            "abobora", "pumpkin", "squash", "moranga", "milho verde", "corn", "agriao",
            "watercress", "repolho", "cabbage", "couve-flor", "cauliflower", "acelga",
            "escarola", "hortela", "mint", "alecrim", "rosemary", "tomilho", "thyme",
            "gengibre", "ginger", "pimenta dedo", "chili", "alho-poro", "leek", "hortifruti"
        )
        if (matches(hortifrutiKeywords)) return ALL[0]

        // 2. Padaria & Confeitaria / Bakery
        val padariaKeywords = listOf(
            "bakery", "bread", "breads", "baguette", "croissant", "brioche", "bun", "buns",
            "roll", "rolls", "bagel", "bagels", "toast", "pao", "baguete", "bolo", "cake",
            "cakes", "torta", "pie", "torrada", "bisnaga", "broa", "sonho", "donuts", "donut",
            "rosquinha", "chipa", "pao de queijo", "pao frances", "pao de forma", "pao sovado",
            "pao australiano", "pao de batata", "pao sirio", "pita", "bisnaguinha", "rap10",
            "tortilha", "tortilla", "ciabatta", "muffin", "muffins", "pastry", "carolina",
            "cuca doce", "folhado", "empadinha", "esfiha", "massa de pastel", "massa de lasanha",
            "polvilho salgado", "biscoito maisena", "panificadora", "confeitaria"
        )
        if (matches(padariaKeywords)) return ALL[1]

        // 3. Laticínios & Frios / Dairy
        val laticiniosKeywords = listOf(
            "dairy", "milk", "cheese", "cheeses", "cheddar", "mozzarella", "mussarela",
            "parmesan", "parmesao", "butter", "manteiga", "margarine", "margarina",
            "yogurt", "yoghurt", "iogurte", "sour cream", "cream cheese", "ricotta", "ricota",
            "cottage", "cream", "leite", "queijo", "prato", "requeijao", "danone",
            "creme de leite", "leite condensado", "condensed milk", "coalhada",
            "presunto", "ham", "peito de peru", "turkey breast", "salame", "salami",
            "mortadela", "frios", "cold cuts", "gorgonzola", "provolone", "brie", "gouda",
            "blanquet", "nata fresca", "chantilly", "whipped cream", "toddynho", "nescau prontinho",
            "yakult", "egg", "eggs", "ovo", "ovos"
        )
        if (matches(laticiniosKeywords)) return ALL[2]

        // 4. Açougue & Peixaria / Meat & Seafood
        val acougueKeywords = listOf(
            "meat", "meats", "beef", "steak", "steaks", "pork", "poultry", "chicken",
            "turkey", "fish", "fishes", "seafood", "salmon", "tuna", "shrimp", "shrimps",
            "carne", "alcatra", "picanha", "maminha", "contrafile", "contra file", "acem",
            "costela", "ribs", "fraldinha", "coxao", "patinho", "moida", "ground beef",
            "bife", "file mignon", "cupim", "lagarto", "musculo", "rabada", "figado bovino",
            "frango", "peito de frango", "chicken breast", "sassami", "coxa", "sobrecoxa",
            "asa de frango", "wings", "tulipa", "file de frango", "coracao de frango",
            "porco", "suino", "lombo", "pernil", "bisteca", "pork chop", "bacon", "linguica",
            "sausage", "calabresa", "carne seca", "charque", "torresmo", "peixe",
            "salmao", "tilapia", "camarao", "sardinha fresca", "atum fresco", "bacalhau", "cod",
            "polvo", "octopus", "lula", "squid", "mexilhao", "merluza", "acougue", "peixaria"
        )
        if (matches(acougueKeywords)) return ALL[3]

        // 5. Mercearia & Grãos / Pantry & Grains
        val merceariaKeywords = listOf(
            "pantry", "grains", "rice", "beans", "pasta", "spaghetti", "penne", "noodles",
            "macarrao", "arroz", "feijao", "fusilli", "parafuso", "talharim", "miojo",
            "oil", "oleo", "cooking oil", "olive oil", "azeite", "salt", "sal", "sugar", "acucar",
            "flour", "farinha", "trigo", "wheat", "oats", "oatmeal", "aveia", "cereal",
            "cornmeal", "fuba", "polvilho", "tapioca", "maizena", "cornstarch", "yeast", "fermento",
            "molho de tomate", "tomato sauce", "extrato", "passata", "canned", "atum em lata",
            "canned tuna", "sardinha em lata", "palmito em conserva", "azeitona", "olives",
            "cogumelo", "mushroom", "maionese", "mayonnaise", "mayo", "ketchup", "mostarda",
            "mustard", "barbecue", "shoyu", "soy sauce", "vinagre", "vinegar", "lentilha",
            "lentils", "grao de bico", "chickpeas", "quinoa", "tempero", "seasoning",
            "spices", "pimenta do reino", "black pepper", "oregano", "paprica", "curcuma",
            "canela", "cinnamon", "leite de coco", "coconut milk"
        )
        if (matches(merceariaKeywords)) return ALL[4]

        // 6. Congelados & Prontos / Frozen Foods
        val congeladosKeywords = listOf(
            "frozen", "freezer", "ice cream", "sorvete", "pizza congelada", "frozen pizza",
            "hamburguer", "burger", "nuggets", "empanado", "lasanha congelada", "frozen meal",
            "batata frita congelada", "french fries", "fries", "picole", "popsicle",
            "acai", "ice", "gelo", "polpa de fruta", "legumes congelados", "frozen vegetables",
            "pao de alho", "waffle", "waffles", "quibe congelado", "coxinha congelada", "congelados"
        )
        if (matches(congeladosKeywords)) return ALL[5]

        // 7. Bebidas / Beverages
        val bebidasKeywords = listOf(
            "beverage", "beverages", "drink", "drinks", "water", "agua mineral", "agua com gas",
            "sparkling water", "agua de coco", "coconut water", "juice", "juices", "suco",
            "nectar", "soda", "soft drink", "refrigerante", "coca", "coke", "guarana",
            "sprite", "fanta", "tonica", "tonic", "beer", "cerveja", "chopp", "wine",
            "vinho", "champagne", "espumante", "prosecco", "whisky", "vodka", "gin",
            "tea", "cha", "coffee", "cafe", "espresso", "nescafe", "capsula", "nescau em po",
            "toddy", "cappuccino", "energy drink", "energetico", "red bull", "monster",
            "gatorade", "isotonico", "bebidas"
        )
        if (matches(bebidasKeywords)) return ALL[6]

        // 8. Limpeza & Lavanderia / Cleaning
        val limpezaKeywords = listOf(
            "cleaning", "cleaner", "detergent", "detergente", "sabao", "laundry soap",
            "laundry detergent", "amaciante", "fabric softener", "desinfetante", "disinfectant",
            "agua sanitaria", "bleach", "cloro", "alcool", "rubbing alcohol", "sponge", "esponja",
            "palha de aco", "steel wool", "bombril", "vassoura", "broom", "rodo", "mop",
            "pano de chao", "microfiber", "saco de lixo", "trash bags", "garbage bags",
            "multiuso", "multi-surface", "limpa vidro", "glass cleaner", "desengordurante",
            "degreaser", "lava-loucas", "dish soap", "dishwasher", "luva", "gloves",
            "inseticida", "insecticide", "aromatizador", "air freshener", "limpeza"
        )
        if (matches(limpezaKeywords)) return ALL[7]

        // 9. Higiene Pessoal / Personal Care
        val higieneKeywords = listOf(
            "personal care", "hygiene", "toilet paper", "papel higienico", "soap", "sabonete",
            "bar soap", "liquid soap", "shampoo", "condicionador", "conditioner",
            "hair mask", "creme dental", "pasta de dente", "toothpaste", "escova de dente",
            "toothbrush", "fio dental", "dental floss", "enxaguante", "mouthwash",
            "desodorante", "deodorant", "absorvente", "tampon", "pads", "barbeador", "razor",
            "gilete", "shaving cream", "espuma de barbear", "algodao", "cotton",
            "cotonete", "q-tips", "hidratante", "body lotion", "moisturizer", "protetor solar",
            "sunscreen", "lenco umedecido", "wet wipes", "fralda", "diaper", "diapers",
            "curativo", "band-aid", "repelente", "repellent", "higiene"
        )
        if (matches(higieneKeywords)) return ALL[8]

        // 10. Petiscos & Doces / Snacks & Sweets
        val petiscosKeywords = listOf(
            "snacks", "snack", "sweets", "candy", "candies", "chocolate", "chocolates",
            "bombom", "kit kat", "m&ms", "biscoito recheado", "oreo", "wafer", "salgadinho",
            "chips", "potato chips", "batata ruffles", "lays", "doritos", "cheetos",
            "pipoca", "popcorn", "amendoim", "peanuts", "peanut", "castanha", "nuts",
            "cashews", "almonds", "nozes", "walnuts", "bala", "gummies", "gummy",
            "chiclete", "gum", "trident", "doce de leite", "nutella", "pasta de amendoim",
            "peanut butter", "mel de abelha", "honey", "geleia", "jam", "jelly",
            "barra de cereal", "granola bar", "cookies", "petiscos", "doces"
        )
        if (matches(petiscosKeywords)) return ALL[9]

        // Default: Outros (Utilidades, Pet Shop, Bazar, Other)
        return ALL[10]
    }
}
