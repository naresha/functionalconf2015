import groovy.json.JsonSlurper
import ratpack.exec.Blocking
import ratpack.exec.Throttle
import ratpack.groovy.template.MarkupTemplateModule
import ratpack.http.client.HttpClient
import ratpack.stream.Streams

import static ratpack.groovy.Groovy.groovyMarkupTemplate
import static ratpack.groovy.Groovy.ratpack
import static ratpack.jackson.Jackson.json

ratpack {


  handlers {
    get {
      render "Hello"
    }

    get("outlets"){
      println "Before blocking" + Thread.currentThread()
      Blocking.get{
        println "Inside Blocking" + Thread.currentThread()
        getOutlets()
      }.then{ outlets ->
        println "Inside then" + Thread.currentThread()
        render json(outlets)
      }
      println "after then" + Thread.currentThread()

    }

    get("products/:id"){ HttpClient httpClient ->
      fetchProduct(httpClient, pathTokens['id'] as Long)
      .map(transformToProductMaster)
      .flatMap{ ProductMaster productMaster ->
        fetchPrice(httpClient, productMaster.id)
        .map(transformToPrice)
        .map { ProductPrice price ->
          transformToProduct(productMaster, price)
        }
      }
      .then{
        render json(it)
      }
    }

    get("products"){HttpClient httpClient ->
      def productsIds = [1, 2, 3]
      Streams.publish(productsIds)
      .flatMap {
        fetchProduct(httpClient, it)
                .map(transformToProductMaster)
      }
        .flatMap{ ProductMaster productMaster ->
          fetchPrice(httpClient, productMaster.id)
                  .map(transformToPrice)
                  .map { ProductPrice price ->
            transformToProduct(productMaster, price)
          }
        }

      .toList()
      .then{
        render json(it)
      }
    }

    post("orders"){
      request.body.map{
        it.text
      }
      .then{
        render it
      }
    }

    get("lazy"){
      Blocking.get{
        println "In blocking"
        "Hello"
      }
      .then{
        render it
      }

    }

    files { dir "public" }
  }
}

def fetchProduct(HttpClient httpClient, Long id){
  httpClient.get(URI.create("http://localhost:5051/products/$id"))
  .throttled(Throttle.ofSize(20))
}

def fetchPrice(HttpClient httpClient, Long id){
  httpClient.get(URI.create("http://localhost:5051/price/$id"))
}

class Outlet{
  Long id
  String name
}

List<Outlet> getOutlets(){
  [
          new Outlet(id: 1, name: 'Express Stores'),
          new Outlet(id: 2, name: 'Food Now'),
          new Outlet(id: 3, name: 'Super Fresh')
  ]
}

class ProductMaster{
  Long id
  String name
}

class ProductPrice{
  Long productId
  BigDecimal mrp
  BigDecimal sellingPrice
}

class Product{
  Long id
  String name
  BigDecimal mrp
  BigDecimal sellingPrice
}

transformToProductMaster = {
  def json = new JsonSlurper().parse(it.body.bytes)
  new ProductMaster(id: json.id, name: json.name)
}

transformToPrice = {
  def json = new JsonSlurper().parse(it.body.bytes)
  new ProductPrice(productId: json.id, mrp: json.mrp, sellingPrice: json.sellingPrice)
}

transformToProduct = { ProductMaster productMaster, ProductPrice price ->
  new Product(id: productMaster.id, name: productMaster.name, mrp: price.mrp, sellingPrice: price.sellingPrice)
}
