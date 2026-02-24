import { graphqlQuery } from './api';

// PUBLIC QUERY - No token required
export const getCategories = async () => {
  const query = `
    query {
      categories {
        categoryId
        name
        description
      }
    }
  `;
  return await graphqlQuery(query);
};

// PROTECTED QUERY - Requires JWT token
export const getProducts = async () => {
  const query = `
    query GetProducts {
      products {
        productId
        productName
        description
        price
        categoryId
      }
    }
  `;
  return await graphqlQuery(query);
};

// PROTECTED MUTATION - Requires JWT token
export const createProduct = async (input) => {
  const mutation = `
    mutation CreateProduct($input: CreateProductInput!) {
      createProduct(input: $input) {
        productId
        name
        price
      }
    }
  `;
  return await graphqlQuery(mutation, { input });
};
