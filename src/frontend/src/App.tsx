import { Header, ShoppingCart } from './components';
import { AiActivityPanel } from './components/AiActivityPanel';
import { NutritionChat } from './components/NutritionChat';
import { Container } from 'react-bootstrap';

function App() {
  return (
    <Container style={{ maxWidth: '600px' }}>
      <Header title='Shopping Cart' />
      <AiActivityPanel />
      <ShoppingCart />
      <NutritionChat />
    </Container>
  );
}

export default App;
