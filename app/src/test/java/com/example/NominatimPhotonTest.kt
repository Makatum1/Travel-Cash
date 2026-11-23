package com.example

import com.example.data.remote.NominatimService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NominatimPhotonTest {

    @Test
    fun photon_campingHuttopia_parsesPoiNameAndFormattedAddress() {
        val jsonStr = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-1.0825, 45.0789]
                  },
                  "properties": {
                    "osm_id": 9876543,
                    "osm_type": "W",
                    "osm_key": "tourism",
                    "osm_value": "camp_site",
                    "name": "Camping Huttopia Lac de Carcans",
                    "street": "Rue du Camping",
                    "housenumber": "1",
                    "postcode": "33121",
                    "city": "Carcans",
                    "county": "Gironde",
                    "state": "Nouvelle-Aquitaine",
                    "country": "France"
                  }
                }
              ]
            }
        """.trimIndent()

        val results = NominatimService.parsePhotonGeoJson(jsonStr, query = "camping huttopia")
        assertEquals(1, results.size)
        val s = results.first()
        assertEquals("Camping Huttopia Lac de Carcans", s.shortPlaceName)
        assertEquals("Rue du Camping, 1, 33121 Carcans, Gironde, Nouvelle-Aquitaine, France", s.formattedAddress)
        assertEquals("Camping Huttopia Lac de Carcans, Rue du Camping, 1, 33121 Carcans, Gironde, Nouvelle-Aquitaine, France", s.displayName)
        assertEquals(45.0789, s.latitude, 1e-6)
        assertEquals(-1.0825, s.longitude, 1e-6)
        assertEquals("Rue du Camping, 1, 33121 Carcans, Gironde, Nouvelle-Aquitaine, France", s.resolveFormattedAddress())
    }

    @Test
    fun photon_peregrinusDezae_parsesPoiAndCity() {
        val jsonStr = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-8.1134, 42.6612]
                  },
                  "properties": {
                    "osm_id": 11223344,
                    "osm_type": "N",
                    "name": "Asoc. Peregrinus Dezae",
                    "street": "Rúa Banda do Río",
                    "housenumber": "12",
                    "postcode": "36500",
                    "city": "Lalín",
                    "county": "Deza",
                    "state": "Pontevedra",
                    "country": "España"
                  }
                }
              ]
            }
        """.trimIndent()

        val results = NominatimService.parsePhotonGeoJson(jsonStr, query = "peregrinus dezae")
        assertEquals(1, results.size)
        val s = results.first()
        assertEquals("Asoc. Peregrinus Dezae", s.shortPlaceName)
        assertEquals("Rúa Banda do Río, 12, 36500 Lalín, Deza, Pontevedra, España", s.formattedAddress)
        assertEquals(42.6612, s.latitude, 1e-6)
        assertEquals(-8.1134, s.longitude, 1e-6)
    }

    @Test
    fun photon_streetOnlyWithoutPoiName_usesStreetAsTitleAndAddressAsAddress() {
        val jsonStr = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-3.7038, 40.4168]
                  },
                  "properties": {
                    "street": "Gran Vía",
                    "housenumber": "28",
                    "postcode": "28013",
                    "city": "Madrid",
                    "country": "España"
                  }
                }
              ]
            }
        """.trimIndent()

        val results = NominatimService.parsePhotonGeoJson(jsonStr, query = "Gran Vía")
        assertEquals(1, results.size)
        val s = results.first()
        assertEquals("Gran Vía, 28", s.shortPlaceName)
        assertEquals("28013 Madrid, España", s.formattedAddress)
    }

    @Test
    fun photon_deduplicatesCoordinates() {
        val jsonStr = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-3.7038, 40.4168]
                  },
                  "properties": {
                    "name": "Sitio 1",
                    "city": "Madrid"
                  }
                },
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-3.7038, 40.4168]
                  },
                  "properties": {
                    "name": "Sitio 1 Duplicado",
                    "city": "Madrid"
                  }
                }
              ]
            }
        """.trimIndent()

        val results = NominatimService.parsePhotonGeoJson(jsonStr)
        assertEquals(1, results.size)
    }
}
