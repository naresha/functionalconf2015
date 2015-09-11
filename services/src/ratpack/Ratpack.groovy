import ratpack.path.PathTokens

import static ratpack.groovy.Groovy.ratpack
import static ratpack.jackson.Jackson.json

ratpack {

  handlers {
    get("products/:id"){
      render json(getProduct(pathTokens['id'] as Long))
    }
    get("price/:id"){
      render json(getPrice(pathTokens['id'] as Long))
    }

    files { dir "public" }
  }
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

ProductMaster getProduct(Long id){
  [
          new ProductMaster(id: 1, name: 'Product 1'),
          new ProductMaster(id: 2, name: 'Product 2'),
          new ProductMaster(id: 3, name: 'Product 3'),
  ].find { it.id == id}
}

ProductPrice getPrice(Long productId){
  [
          new ProductPrice(productId: 1, mrp: 100.00, sellingPrice: 80.00),
          new ProductPrice(productId: 2, mrp: 200.00, sellingPrice: 180.50),
          new ProductPrice(productId: 3, mrp: 50.00, sellingPrice: 41.25)
  ].find { it.productId == productId}
}
