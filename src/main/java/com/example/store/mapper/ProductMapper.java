package com.example.store.mapper;

import java.util.List;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDTO productToProductDTO(Product product);

    List<ProductDTO> productsToProductDTOs(List<Product> products);

}
