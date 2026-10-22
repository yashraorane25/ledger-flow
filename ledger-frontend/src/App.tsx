 import { useState } from 'react';                                                                                                                                                  
    import './App.css';                                                                                                                                                                
    import { PaymentManager } from './components/PaymentManager';                                                                                                                      
    import { LedgerManager } from './components/LedgerManager';                                                                                                                        
    import { ReconciliationManager } from './components/ReconciliationManager';                                                                                                        
                                                                                                                                                                                       
    export default function App() {                                                                                                                                                    
      const [ledgerRefreshKey, setLedgerRefreshKey] = useState(0);                                                                                                                     
                                                                                                                                                                                       
      const handlePaymentProcessed = () => {                                                                                                                                           
        // Automatically trigger balance refresh when a payment completes                                                                                                              
        setLedgerRefreshKey((k) => k + 1);                                                                                                                                             
      };                                                                                                                                                                               
                                                                                                                                                                                       
      return (                                                                                                                                                                         
        <div className="container">                                                                                                                                                    
          <header className="header">                                                                                                                                                  
            <h1>Ledger-Flow Operations Console</h1>                                                                                                                                    
            <p>Event-Driven Payment Settlement & Double-Entry Accounting</p>                                                                                                           
          </header>                                                                                                                                                                    
                                                                                                                                                                                       
          <main className="content">                                                                                                                                                   
            <PaymentManager onPaymentProcessed={handlePaymentProcessed} />                                                                                                             
            <LedgerManager refreshKey={ledgerRefreshKey} />                                                                                                                            
            <ReconciliationManager />                                                                                                                                                  
          </main>                                                                                                                                                                      
        </div>                                                                                                                                                                         
      );                                                                                                                                                                               
    } 