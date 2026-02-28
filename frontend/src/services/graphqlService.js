import { GraphQLClient, gql } from 'graphql-request';

const GRAPHQL_URL = process.env.REACT_APP_GRAPHQL_URL || 'http://localhost:8080/graphql';

const client = new GraphQLClient(GRAPHQL_URL);

const graphqlService = {
  // Fetch all products
  getAllProducts: async () => {
    const query = gql`
      query {
        products {
          productId
          productName
          description
          price
          categoryId
          categoryName
          createdAt

        }
      }
    `;
    const data = await client.request(query);
    return data.products;
  },

  // Fetch products with filters
  getProducts: async (params = {}) => {
    const { category, minPrice, maxPrice, searchTerm } = params;
    
    const query = gql`
      query productsPaged($category: String, $minPrice: Float, $maxPrice: Float, $searchTerm: String) {
        products(
          category: $category
          minPrice: $minPrice
          maxPrice: $maxPrice
          searchTerm: $searchTerm
        ) {
          productId
          productName
          description
          price
          categoryId
          categoryName
          createdAt
        }
      }
    `;
    
    const variables = {
      category: category || null,
      minPrice: minPrice ? parseFloat(minPrice) : null,
      maxPrice: maxPrice ? parseFloat(maxPrice) : null,
      searchTerm: searchTerm || null,
    };
    
    const data = await client.request(query, variables);
    return data.products;
  },

  // Fetch single product by ID
  getProductById: async (id) => {
    const query = gql`
      query GetProduct($id: Int!) {
        product(id: $id) {
          productId
          productName
          description
          price
          categoryId
          categoryName
          createdAt
        }
      }
    `;
    const data = await client.request(query, { id: parseInt(id) });
    return data.product;
  },

  // Fetch all categories
  getAllCategories: async () => {
    const query = gql`
      query {
        categories {
          categoryId
          categoryName
          description
          createdAt
        }
      }
    `;
    const data = await client.request(query);
    return data.categories;
  },

  // Fetch single category by ID
  getCategoryById: async (id) => {
    const query = gql`
      query GetCategory($id: Int!) {
        category(id: $id) {
          categoryId
          categoryName
          description
          createdAt
        }
      }
    `;
    const data = await client.request(query, { id: parseInt(id) });
    return data.category;
  },
};

export default graphqlService;
