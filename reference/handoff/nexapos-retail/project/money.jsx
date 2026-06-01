/* money.jsx — Cash & Bank, Income, Expense, Ledger */
const { useState: umyS } = React;

const ACCOUNTS = [
  {name:"Till · Counter 01", type:"cash",  bal:28420, icon:"cash",  c:"var(--amber)"},
  {name:"Till · Counter 02", type:"cash",  bal:12860, icon:"cash",  c:"var(--amber)"},
  {name:"MCB Current A/C",   type:"bank",  bal:284600,icon:"wallet",c:"var(--ink)"},
  {name:"MCB Savings",       type:"bank",  bal:180000,icon:"wallet",c:"var(--ink)"},
  {name:"Juice Business",    type:"mobile",bal:14280, icon:"mobile",c:"var(--emerald)"},
];

const EXPENSES = [
  {id:"E-042",d:"26 May",cat:"Rent",desc:"Shop rent · May 2026",amt:35000,acc:"MCB Current",who:"S. Khan"},
  {id:"E-041",d:"25 May",cat:"Salary",desc:"Staff salaries (4)",amt:48000,acc:"MCB Current",who:"Sameer K."},
  {id:"E-040",d:"24 May",cat:"Utilities",desc:"CEB electricity",amt:8200,acc:"MCB Current",who:"Sameer K."},
  {id:"E-039",d:"22 May",cat:"Transport",desc:"Delivery van fuel",amt:3400,acc:"Till 01",who:"Driver R."},
  {id:"E-038",d:"20 May",cat:"Maintenance",desc:"AC repair shop",amt:4500,acc:"Till 01",who:"S. Khan"},
  {id:"E-037",d:"18 May",cat:"Supplies",desc:"Packaging, labels",amt:1850,acc:"Juice",who:"S. Khan"},
];

const INCOMES = [
  {id:"I-088",d:"26 May",cat:"Sales",desc:"Day sales · Counter 01",amt:68420,acc:"Till 01",who:"auto"},
  {id:"I-087",d:"26 May",cat:"Sales",desc:"Day sales · Counter 02",amt:32100,acc:"Till 02",who:"auto"},
  {id:"I-086",d:"25 May",cat:"Sales",desc:"Day sales combined",amt:54200,acc:"Till 01",who:"auto"},
  {id:"I-085",d:"24 May",cat:"Rental",desc:"Warehouse sublease",amt:15000,acc:"MCB Current",who:"Sameer K."},
  {id:"I-084",d:"22 May",cat:"Sales",desc:"Day sales combined",amt:61800,acc:"Till 01",who:"auto"},
];

const LEDGER = [
  {d:"26 May 14:08",ref:"S-00010",type:"sale",desc:"Ravi Soobramoney · 3 items",dr:2145,cr:0,bal:28420},
  {d:"26 May 13:42",ref:"S-00009",type:"sale",desc:"Walk-in · cash",dr:3240,cr:0,bal:26275},
  {d:"26 May 12:55",ref:"S-00008",type:"sale",desc:"Chemtex Co. · credit",dr:0,cr:0,bal:23035},
  {d:"26 May 09:30",ref:"PO-1042",type:"purchase",desc:"Ducray Hardware · bank",dr:0,cr:48990,bal:23035},
  {d:"25 May 18:22",ref:"S-00003",type:"sale",desc:"Walk-in · cash",dr:140,cr:0,bal:72025},
  {d:"25 May 17:55",ref:"R-007",type:"refund",desc:"Walk-in refund",dr:0,cr:480,bal:71885},
  {d:"25 May 17:01",ref:"S-00001",type:"sale",desc:"V. Ramphul · card",dr:4485,cr:0,bal:72365},
  {d:"25 May",ref:"E-041",type:"expense",desc:"Staff salaries",dr:0,cr:48000,bal:67880},
];

/* ============================================================
   Cash & Bank (Money hub)
   ============================================================ */
function CashBank({ theme, onTheme, onGoTo }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="money"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Money" subtitle="5 accounts · Rs 520.16K total balance"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.plus size={14}/> Add account</button>
          </div>}/>

        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          {/* Nav tabs */}
          <div style={{display:"flex",gap:0,background:"var(--raised)",border:"1px solid var(--hairline)",borderRadius:10,padding:4,marginBottom:18,width:"fit-content"}}>
            {[{id:"accounts",l:"Cash & Bank"},{id:"income",l:"Income"},{id:"expense",l:"Expenses"},{id:"ledger",l:"Ledger"}].map((t,i)=>(
              <button key={t.id} onClick={()=>onGoTo&&onGoTo(t.id==="accounts"?"money":t.id==="income"?"income":"expense"===t.id?"expense":"ledger")}
                style={{height:36,padding:"0 18px",border:0,borderRadius:8,background:i===0?"var(--ink)":"transparent",color:i===0?"var(--surface)":"var(--ink)",fontSize:13,fontWeight:700,cursor:"pointer"}}>{t.l}</button>
            ))}
          </div>

          {/* Total band */}
          <div className="card-machined" style={{padding:20,marginBottom:18}}>
            <div style={{display:"grid",gridTemplateColumns:"repeat(4,1fr)",gap:18}}>
              <div><div className="eyebrow">Total balance</div><div className="num" style={{fontSize:32,fontWeight:800,marginTop:4}}>Rs 520,160</div></div>
              <div><div className="eyebrow">Cash in hand</div><div className="num" style={{fontSize:22,fontWeight:700,marginTop:4}}>Rs 41,280</div><div className="num" style={{fontSize:11,color:"var(--muted)"}}>2 tills</div></div>
              <div><div className="eyebrow">Bank</div><div className="num" style={{fontSize:22,fontWeight:700,marginTop:4}}>Rs 464,600</div><div className="num" style={{fontSize:11,color:"var(--muted)"}}>2 accounts</div></div>
              <div><div className="eyebrow">Mobile wallet</div><div className="num" style={{fontSize:22,fontWeight:700,marginTop:4}}>Rs 14,280</div><div className="num" style={{fontSize:11,color:"var(--muted)"}}>Juice Business</div></div>
            </div>
          </div>

          {/* Accounts */}
          <div className="eyebrow" style={{marginBottom:10}}>Accounts</div>
          <div style={{display:"grid",gridTemplateColumns:"repeat(3,1fr)",gap:14}} className="reveal">
            {ACCOUNTS.map((a,i)=>{const IcC=Icon[a.icon]; return (
              <div key={i} className="card-machined" style={{padding:18,cursor:"pointer"}} onClick={()=>onGoTo&&onGoTo("ledger")}>
                <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
                  <div style={{width:42,height:42,borderRadius:12,background:a.c,color:"var(--surface)",display:"flex",alignItems:"center",justifyContent:"center"}}><IcC size={18}/></div>
                  <span className="badge badge-ghost" style={{textTransform:"capitalize"}}>{a.type}</span>
                </div>
                <div style={{fontSize:15,fontWeight:700,marginTop:12}}>{a.name}</div>
                <div className="num" style={{fontSize:26,fontWeight:800,marginTop:6,letterSpacing:"-0.02em"}}>Rs {a.bal.toLocaleString()}</div>
                <div style={{display:"flex",gap:8,marginTop:12}}>
                  <button className="btn btn-secondary btn-sm" style={{flex:1}}>Transfer</button>
                  <button className="btn btn-ghost btn-sm" style={{flex:1}}>History</button>
                </div>
              </div>
            )})}
          </div>

          {/* Recent */}
          <div className="eyebrow" style={{marginTop:22,marginBottom:10}}>Recent transactions</div>
          <div className="card" style={{padding:0,overflow:"hidden"}}>
            {LEDGER.slice(0,5).map((e,i)=>(
              <div key={i} style={{display:"flex",alignItems:"center",gap:14,padding:"12px 18px",borderBottom:"1px solid var(--hairline-2)"}}>
                <div style={{width:38,height:38,borderRadius:10,background:e.type==="sale"?"var(--emerald-soft)":e.type==="refund"?"var(--crimson-soft)":"var(--raised-2)",color:e.type==="sale"?"var(--emerald)":e.type==="refund"?"var(--crimson)":"var(--ink)",display:"flex",alignItems:"center",justifyContent:"center"}}>
                  {e.type==="sale"?<Icon.arrowup size={16}/>:e.type==="refund"?<Icon.arrowdn size={16}/>:e.type==="purchase"?<Icon.truck size={16}/>:<Icon.wallet size={16}/>}
                </div>
                <div style={{flex:1}}>
                  <div style={{fontSize:13,fontWeight:600}}>{e.desc}</div>
                  <div className="num" style={{fontSize:11,color:"var(--muted)"}}>{e.ref} · {e.d}</div>
                </div>
                {e.dr>0 && <span className="num" style={{fontSize:14,fontWeight:700,color:"var(--emerald)"}}>+ Rs {e.dr.toLocaleString()}</span>}
                {e.cr>0 && <span className="num" style={{fontSize:14,fontWeight:700,color:"var(--crimson)"}}>- Rs {e.cr.toLocaleString()}</span>}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Expense list
   ============================================================ */
function ExpenseList({ theme, onTheme, onGoTo }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="money"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Expenses" subtitle="Rs 100.95K this month · 6 entries"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={()=>onGoTo&&onGoTo("money")}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Money</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm" onClick={()=>onGoTo&&onGoTo("add-expense")}><Icon.plus size={14}/> Record expense</button>
          </div>}/>
        <div style={{padding:"14px 22px 0",display:"flex",gap:6}}>
          {["All","Rent","Salary","Utilities","Transport","Maintenance","Supplies"].map((c,i)=>(
            <button key={c} className={"chip"+(i===0?" active":"")}>{c}</button>
          ))}
        </div>
        <div style={{padding:"14px 22px 22px",flex:1,overflow:"hidden"}}>
          <div className="card" style={{height:"100%",display:"flex",flexDirection:"column",overflow:"hidden"}}>
            <div style={puTh}>
              <div style={{width:80}}>ID</div>
              <div style={{width:100}}>Date</div>
              <div style={{width:100}}>Category</div>
              <div style={{flex:1}}>Description</div>
              <div style={{width:130}}>Account</div>
              <div style={{width:120,textAlign:"right"}}>Amount</div>
              <div style={{width:100}}>By</div>
            </div>
            <div className="scroll reveal" style={{flex:1,overflow:"auto"}}>
              {EXPENSES.map(e=>(
                <div key={e.id} style={puTr}>
                  <div className="num" style={{width:80,fontWeight:700,fontSize:13}}>{e.id}</div>
                  <div className="num" style={{width:100,fontSize:12,color:"var(--muted)"}}>{e.d}</div>
                  <div style={{width:100}}><span className="badge badge-ghost">{e.cat}</span></div>
                  <div style={{flex:1,fontSize:13,fontWeight:600}}>{e.desc}</div>
                  <div style={{width:130,fontSize:12,color:"var(--graphite)"}}>{e.acc}</div>
                  <div className="num" style={{width:120,textAlign:"right",fontSize:15,fontWeight:700,color:"var(--crimson)"}}>Rs {e.amt.toLocaleString()}</div>
                  <div style={{width:100,fontSize:12,color:"var(--muted)"}}>{e.who}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Income list
   ============================================================ */
function IncomeList({ theme, onTheme, onGoTo }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="money"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Income" subtitle="Rs 231.52K this month"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={()=>onGoTo&&onGoTo("money")}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Money</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.plus size={14}/> Add income</button>
          </div>}/>
        <div style={{padding:"14px 22px 22px",flex:1,overflow:"hidden"}}>
          <div className="card" style={{height:"100%",display:"flex",flexDirection:"column",overflow:"hidden"}}>
            <div style={puTh}>
              <div style={{width:80}}>ID</div>
              <div style={{width:100}}>Date</div>
              <div style={{width:100}}>Category</div>
              <div style={{flex:1}}>Description</div>
              <div style={{width:130}}>Account</div>
              <div style={{width:120,textAlign:"right"}}>Amount</div>
            </div>
            <div className="scroll reveal" style={{flex:1,overflow:"auto"}}>
              {INCOMES.map(e=>(
                <div key={e.id} style={puTr}>
                  <div className="num" style={{width:80,fontWeight:700,fontSize:13}}>{e.id}</div>
                  <div className="num" style={{width:100,fontSize:12,color:"var(--muted)"}}>{e.d}</div>
                  <div style={{width:100}}><span className="badge badge-paid">{e.cat}</span></div>
                  <div style={{flex:1,fontSize:13,fontWeight:600}}>{e.desc}</div>
                  <div style={{width:130,fontSize:12,color:"var(--graphite)"}}>{e.acc}</div>
                  <div className="num" style={{width:120,textAlign:"right",fontSize:15,fontWeight:700,color:"var(--emerald)"}}>Rs {e.amt.toLocaleString()}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Add Expense
   ============================================================ */
function AddExpense({ theme, onTheme, onBack }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="money"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Record Expense" subtitle="Add a new operating expense"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-ghost btn-sm" onClick={onBack}>Cancel</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            <button className="btn btn-primary btn-sm"><Icon.check size={14}/> Save</button>
          </div>}/>
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"18px 22px 28px"}}>
          <div style={{maxWidth:680,margin:"0 auto",display:"flex",flexDirection:"column",gap:14}}>
            <div className="card-machined" style={{padding:20}}>
              <div className="eyebrow" style={{marginBottom:14}}>Expense details</div>
              <div style={{display:"grid",gridTemplateColumns:"1fr 1fr",gap:12}}>
                <div><div className="label">Category</div><div className="field"><select style={{flex:1,border:0,background:"transparent",fontFamily:"var(--display)",fontSize:14,color:"var(--ink)"}}><option>Rent</option><option>Salary</option><option>Utilities</option><option>Transport</option><option>Maintenance</option><option>Supplies</option></select></div></div>
                <div><div className="label">Date</div><div className="field"><input defaultValue="26 May 2026"/></div></div>
                <div style={{gridColumn:"span 2"}}><div className="label">Description</div><div className="field"><input defaultValue=""/></div></div>
                <div><div className="label">Amount</div><div className="field"><span style={{color:"var(--muted)",fontSize:13}}>Rs</span><input className="num" defaultValue="" style={{textAlign:"right",fontWeight:600}}/></div></div>
                <div><div className="label">Pay from account</div><div className="field"><select style={{flex:1,border:0,background:"transparent",fontFamily:"var(--display)",fontSize:14,color:"var(--ink)"}}><option>Till · Counter 01</option><option>MCB Current</option><option>Juice Business</option></select></div></div>
                <div style={{gridColumn:"span 2"}}><div className="label">Notes (optional)</div><div className="field" style={{height:"auto",padding:10}}><textarea rows="2" style={{background:"transparent",border:0,outline:0,resize:"none",width:"100%",fontFamily:"var(--display)"}}/></div></div>
              </div>
            </div>
            <div style={{display:"flex",justifyContent:"flex-end",gap:8}}>
              <button className="btn btn-secondary" onClick={onBack}>Cancel</button>
              <button className="btn btn-primary"><Icon.check size={16}/> Save expense</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   Ledger
   ============================================================ */
function LedgerView({ theme, onTheme, onGoTo }) {
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="money"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0,overflow:"hidden"}}>
        <AppBar title="Ledger" subtitle="All journal entries · Till · Counter 01"
          right={<div style={{display:"flex",gap:8}}>
            <button className="btn btn-secondary btn-sm" onClick={()=>onGoTo&&onGoTo("money")}><Icon.arrow_r size={14} style={{transform:"rotate(180deg)"}}/> Money</button>
            <button className="btn btn-secondary btn-sm"><Icon.filter size={14}/> Today</button>
            <button className="btn btn-secondary btn-sm"><Icon.download size={14}/> Export</button>
            <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
          </div>}/>
        <div style={{padding:"14px 22px 22px",flex:1,overflow:"hidden"}}>
          <div className="card" style={{height:"100%",display:"flex",flexDirection:"column",overflow:"hidden"}}>
            <div style={puTh}>
              <div style={{width:140}}>Date</div>
              <div style={{width:100}}>Ref</div>
              <div style={{width:80}}>Type</div>
              <div style={{flex:1}}>Description</div>
              <div style={{width:120,textAlign:"right"}}>Debit</div>
              <div style={{width:120,textAlign:"right"}}>Credit</div>
              <div style={{width:120,textAlign:"right"}}>Balance</div>
            </div>
            <div className="scroll reveal" style={{flex:1,overflow:"auto"}}>
              {LEDGER.map((e,i)=>(
                <div key={i} style={puTr}>
                  <div className="num" style={{width:140,fontSize:12,color:"var(--muted)"}}>{e.d}</div>
                  <div className="num" style={{width:100,fontWeight:700,fontSize:13}}>{e.ref}</div>
                  <div style={{width:80}}><span className={"badge "+(e.type==="sale"?"badge-paid":e.type==="refund"?"badge-due":"badge-ghost")} style={{textTransform:"capitalize"}}>{e.type}</span></div>
                  <div style={{flex:1,fontSize:13,fontWeight:600}}>{e.desc}</div>
                  <div className="num" style={{width:120,textAlign:"right",fontSize:14,fontWeight:e.dr?700:400,color:e.dr?"var(--emerald)":"var(--muted)"}}>{e.dr?`Rs ${e.dr.toLocaleString()}`:"—"}</div>
                  <div className="num" style={{width:120,textAlign:"right",fontSize:14,fontWeight:e.cr?700:400,color:e.cr?"var(--crimson)":"var(--muted)"}}>{e.cr?`Rs ${e.cr.toLocaleString()}`:"—"}</div>
                  <div className="num" style={{width:120,textAlign:"right",fontSize:14,fontWeight:700}}>Rs {e.bal.toLocaleString()}</div>
                </div>
              ))}
            </div>
            <div style={{padding:"12px 18px",borderTop:"1px solid var(--hairline)",background:"var(--surface)",display:"flex",justifyContent:"space-between",alignItems:"center"}}>
              <div className="num" style={{fontSize:12,color:"var(--muted)"}}>Showing 8 entries · opening balance Rs 115,880</div>
              <div style={{display:"flex",gap:18}}>
                <div><span style={{fontSize:11,color:"var(--muted)"}}>Total debit</span> <span className="num" style={{fontWeight:700,color:"var(--emerald)"}}>Rs 10,010</span></div>
                <div><span style={{fontSize:11,color:"var(--muted)"}}>Total credit</span> <span className="num" style={{fontWeight:700,color:"var(--crimson)"}}>Rs 97,470</span></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { CashBank, ExpenseList, IncomeList, AddExpense, LedgerView });
