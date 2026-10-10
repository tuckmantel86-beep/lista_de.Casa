package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entities.ShoppingItemEntity
import com.example.data.model.CatalogData
import com.example.data.model.Categories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Lista da Casa", appName)
    val webClientId = context.getString(R.string.default_web_client_id)
    assertTrue(webClientId.isNotEmpty())
    assertTrue(webClientId.contains("googleusercontent.com"))
  }

  @Test
  fun `verify catalog has more than 500 items across 11 categories`() {
    assertTrue(CatalogData.ITEMS.size >= 500)
    assertEquals(11, Categories.ALL.size)
    assertTrue(CatalogData.ESSENTIALS.isNotEmpty())
  }

  @Test
  fun `verify accent insensitive catalog search`() {
    val acucarResults = CatalogData.search("acucar")
    assertTrue(acucarResults.any { it.name.contains("Açúcar", ignoreCase = true) })

    val cafeResults = CatalogData.search("cafe")
    assertTrue(cafeResults.any { it.name.contains("Café", ignoreCase = true) })

    val paoResults = CatalogData.search("pao")
    assertTrue(paoResults.any { it.name.contains("Pão", ignoreCase = true) })

    val macaResults = CatalogData.search("maca")
    assertTrue(macaResults.any { it.name.contains("Maçã", ignoreCase = true) })
  }

  @Test
  fun `verify smart category detection`() {
    val bananaCat = Categories.detectCategory("Banana Nanica")
    assertEquals("hortifruti", bananaCat.id)

    val picanhaCat = Categories.detectCategory("Picanha Angus")
    assertEquals("acougue", picanhaCat.id)

    val paoCat = Categories.detectCategory("pao de forma")
    assertEquals("padaria", paoCat.id)

    val sabaoCat = Categories.detectCategory("sabao em po")
    assertEquals("limpeza", sabaoCat.id)

    val acucarCat = Categories.detectCategory("acucar refinado")
    assertEquals("mercearia", acucarCat.id)

    // Bilingual / English keyword detection
    val produceCat = Categories.detectCategory("Fresh Produce Apples")
    assertEquals("hortifruti", produceCat.id)

    val dairyCat = Categories.detectCategory("Organic Whole Milk")
    assertEquals("laticinios", dairyCat.id)

    val bakeryCat = Categories.detectCategory("Artisan Baguette Bread")
    assertEquals("padaria", bakeryCat.id)

    val meatCat = Categories.detectCategory("Ribeye Steak Beef")
    assertEquals("acougue", meatCat.id)
  }

  @Test
  fun `verify getByName with English category names`() {
    val produce = Categories.getByName("Produce")
    assertEquals("hortifruti", produce.id)
    assertEquals("Produce", produce.englishName)

    val dairy = Categories.getByName("Dairy")
    assertEquals("laticinios", dairy.id)
    assertEquals("Dairy", dairy.englishName)

    val bakery = Categories.getByName("Bakery")
    assertEquals("padaria", bakery.id)
    assertEquals("Bakery", bakery.englishName)
  }

  @Test
  fun `verify intelligent catalog search with English synonyms`() {
    val milkResults = CatalogData.search("milk")
    assertTrue(milkResults.any { it.name.contains("Leite", ignoreCase = true) })

    val breadResults = CatalogData.search("bread")
    assertTrue(breadResults.any { it.name.contains("Pão", ignoreCase = true) })

    val appleResults = CatalogData.search("apple")
    assertTrue(appleResults.any { it.name.contains("Maçã", ignoreCase = true) })
  }

  @Test
  fun `verify item price and count subtotal calculation`() {
    val item = ShoppingItemEntity(
      id = "test-1",
      listId = "list-1",
      houseId = "house-1",
      name = "Banana",
      category = "hortifruti",
      unitPrice = 7.50,
      itemCount = 3
    )
    assertEquals(22.50, item.totalPrice, 0.001)
  }

  @Test
  fun `verify all 11 categories have distinct aisles and valid metadata`() {
    val allAisles = Categories.ALL.map { it.aisleNumber }.toSet()
    assertEquals(11, allAisles.size)
    for (i in 1..11) {
      assertTrue("Missing aisle $i", allAisles.contains(i))
    }
  }

  @Test
  fun `verify detectCategory for frozen, drinks, hygiene, and sweets`() {
    val pizza = Categories.detectCategory("Pizza Calabresa Sadia")
    assertEquals("congelados", pizza.id)

    val coca = Categories.detectCategory("Coca Cola 2 Litros")
    assertEquals("bebidas", coca.id)

    val shampoo = Categories.detectCategory("Shampoo Pantene 400ml")
    assertEquals("higiene", shampoo.id)

    val chocolate = Categories.detectCategory("Chocolate Barra Nestle")
    assertEquals("petiscos", chocolate.id)
  }
}
