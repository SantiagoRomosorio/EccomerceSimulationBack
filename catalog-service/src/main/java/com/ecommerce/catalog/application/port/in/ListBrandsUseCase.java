package com.ecommerce.catalog.application.port.in;

import com.ecommerce.catalog.domain.model.Brand;
import java.util.List;

public interface ListBrandsUseCase {

    List<Brand> listBrands();
}
