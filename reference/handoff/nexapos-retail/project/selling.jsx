/* selling.jsx — POS Sale, Checkout, Receipt complete (tablet) */
const { useState: uS, useEffect: uE, useRef: uR, useMemo: uM } = React;

/* sample products (Mauritian quincaillerie) */
const PRODUCTS = [
  {id:"P-1001", name:"Sprayer 20L Hi-Pressure",  cat:"Plumbing", price:1250, sku:"SPR-20L", stock:14, kind:"sprayer"},
  {id:"P-1002", name:"Viseuse Cordless 18V",     cat:"Tools",    price:1450, sku:"VSC-18V", stock:6,  kind:"drill"},
  {id:"P-1003", name:"T-Type Wrench 17mm",       cat:"Tools",    price:185,  sku:"WRN-T17", stock:48, kind:"wrench"},
  {id:"P-1004", name:'Saw Chain 18"',            cat:"Tools",    price:850,  sku:"SAW-018", stock:11, kind:"saw"},
  {id:"P-1005", name:"Frottoire Gros Malaysia",  cat:"Paint",    price:245,  sku:"FRT-GMY", stock:34, kind:"scrubber"},
  {id:"P-1006", name:"Hammer Claw 16oz Forged",  cat:"Tools",    price:320,  sku:"HMR-16",  stock:22, kind:"hammer"},
  {id:"P-1007", name:"PVC Pipe 110mm × 3m",      cat:"Plumbing", price:480,  sku:"PVC-110", stock:3,  kind:"pipe"},
  {id:"P-1008", name:"Paint Enamel 5L Brick",    cat:"Paint",    price:1180, sku:"PNT-5BR", stock:9,  kind:"paint"},
  {id:"P-1009", name:"Self-Tap Screws 4×40",     cat:"Fasteners",price:45,   sku:"SCR-440", stock:520,kind:"generic"},
  {id:"P-1010", name:"Bolt M10 Hex Galv",        cat:"Fasteners",price:18,   sku:"BLT-M10", stock:880,kind:"generic"},
  {id:"P-1011", name:'Pliers Combination 8"',    cat:"Tools",    price:295,  sku:"PLR-08",  stock:17, kind:"wrench"},
  {id:"P-1012", name:"Spray Paint Matt Black",   cat:"Paint",    price:175,  sku:"SPR-MBK", stock:42, kind:"paint"},
];
const CATS = ["All","Plumbing","Tools","Fasteners","Paint","Garden","Electrical"];

/* ---------- Product Card ---------- */
function ProductCard({ p, onAdd }) {
  const ref = uR(null);
  const ripRef = uR(null);
  const handle = (e) => {
    // ripple
    const el = ripRef.current;
    if (el) { el.style.animation = "none"; void el.offsetWidth; el.style.animation = "ripple .6s ease-out"; }
    // pop
    const c = ref.current; if (c) { c.style.animation = "none"; void c.offsetWidth; c.style.animation = "pop .25s ease-out"; }
    onAdd(p, ref.current);
  };
  const low = p.stock <= 6;
  return (
    <button ref={ref} onClick={handle}
      className="card-machined"
      style={{
        position:"relative", padding:14, textAlign:"left", cursor:"pointer",
        border:"1px solid var(--hairline)", overflow:"hidden",
        display:"flex",flexDirection:"column",gap:10,
      }}>
      <span ref={ripRef} style={{
        position:"absolute", inset:"50% 50% auto auto", width:280, height:280, borderRadius:"50%",
        background: "var(--amber)", opacity:0, transform:"translate(50%,-50%) scale(0)",
        pointerEvents:"none", mixBlendMode:"multiply"
      }}/>
      <div style={{position:"relative",display:"flex",alignItems:"center",justifyContent:"center", background:"var(--raised-2)", borderRadius:10, height:96, border:"1px solid var(--hairline-2)"}}>
        <ProductTile kind={p.kind} size={88}/>
        {low && <span className="badge badge-low" style={{position:"absolute",top:8,left:8}}>● {p.stock} left</span>}
      </div>
      <div style={{display:"flex",flexDirection:"column",gap:4}}>
        <div style={{fontSize:13.5,fontWeight:600,lineHeight:1.25,letterSpacing:"-0.005em",
          display:"-webkit-box",WebkitLineClamp:2,WebkitBoxOrient:"vertical",overflow:"hidden",minHeight:34}}>{p.name}</div>
        <div style={{display:"flex",alignItems:"baseline",justifyContent:"space-between",gap:6}}>
          <span className="num" style={{fontSize:18,fontWeight:700,color:"var(--ink)"}}>Rs {p.price.toLocaleString()}</span>
          <span style={{fontSize:10.5,color:"var(--muted)",fontFamily:"var(--mono)",letterSpacing:".02em"}}>{p.sku}</span>
        </div>
      </div>
    </button>
  );
}

/* ---------- Ticket line item ---------- */
function TicketLine({ line, onDec, onInc, onRm }) {
  return (
    <div style={{display:"flex",gap:10,padding:"12px 14px",borderBottom:"1px solid var(--hairline-2)",alignItems:"center"}}>
      <div style={{width:44,height:44,borderRadius:8,background:"var(--raised-2)",border:"1px solid var(--hairline-2)",display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0,overflow:"hidden"}}>
        <ProductTile kind={line.kind} size={40}/>
      </div>
      <div style={{flex:1,minWidth:0}}>
        <div style={{fontSize:13,fontWeight:600,lineHeight:1.2,whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis"}}>{line.name}</div>
        <div style={{fontSize:11,color:"var(--muted)",marginTop:2,fontFamily:"var(--mono)",letterSpacing:".02em"}}>{line.sku} · Rs {line.price.toLocaleString()}</div>
      </div>
      <div style={{display:"flex",alignItems:"center",gap:0,border:"1px solid var(--hairline)",borderRadius:8,overflow:"hidden",background:"var(--raised)"}}>
        <button onClick={onDec} style={btnStep}><Icon.minus size={14}/></button>
        <span className="num" style={{width:30,textAlign:"center",fontSize:14,fontWeight:700}}>{line.qty}</span>
        <button onClick={onInc} style={btnStep}><Icon.plus size={14}/></button>
      </div>
      <div className="num" style={{width:78,textAlign:"right",fontSize:14,fontWeight:700}}>Rs {(line.price*line.qty).toLocaleString()}</div>
      <button onClick={onRm} style={{border:0,background:"transparent",color:"var(--muted)",cursor:"pointer",padding:4}}><Icon.close size={15}/></button>
    </div>
  );
}
const btnStep = {width:30,height:32,border:0,background:"transparent",cursor:"pointer",display:"inline-flex",alignItems:"center",justifyContent:"center",color:"var(--ink)"};

/* ---------- Fly chip animation manager ---------- */
function useFlyChips() {
  const [chips, setChips] = uS([]);
  const fly = (fromEl, toEl) => {
    if (!fromEl || !toEl) return;
    const a = fromEl.getBoundingClientRect();
    const b = toEl.getBoundingClientRect();
    // We need positions relative to a fixed container; use viewport coords
    const id = Math.random().toString(36).slice(2);
    setChips(c => [...c, { id, x: a.left + a.width/2 - 18, y: a.top + 10, tx: b.left + b.width/2 - a.left - a.width/2, ty: b.top - a.top - 10 }]);
    setTimeout(()=>setChips(c => c.filter(x=>x.id!==id)), 700);
  };
  const layer = (
    <div style={{position:"fixed",inset:0,pointerEvents:"none",zIndex:100}}>
      {chips.map(c => (
        <div key={c.id} style={{
          position:"absolute", left: c.x, top: c.y, width: 36, height: 36, borderRadius: 99,
          background:"var(--amber)", color:"#fff", display:"flex",alignItems:"center",justifyContent:"center",
          fontWeight:700, fontSize:14, boxShadow:"0 8px 18px rgba(232,101,29,0.4)",
          ['--tx']: c.tx+"px", ['--ty']: c.ty+"px",
          animation:"fly .65s cubic-bezier(.45,.1,.4,1) forwards"
        }}>+1</div>
      ))}
    </div>
  );
  return [layer, fly];
}

/* ============================================================
   POS SALE — Tablet landscape
   ============================================================ */
function POSSale({ onCharge, onTheme, theme, onGoTo }) {
  const [cat, setCat] = uS("All");
  const [q, setQ] = uS("");
  const [lines, setLines] = uS([
    {...PRODUCTS[2], qty:2, kind:PRODUCTS[2].kind},
    {...PRODUCTS[5], qty:1, kind:PRODUCTS[5].kind},
  ]);
  const [customer, setCustomer] = uS({name:"Ravi Soobramoney", phone:"+230 5712 4408"});
  const cartRef = uR(null);
  const [flyLayer, fly] = useFlyChips();

  const visible = uM(()=>PRODUCTS.filter(p =>
    (cat==="All"||p.cat===cat) &&
    (!q.trim()|| (p.name+" "+p.sku).toLowerCase().includes(q.toLowerCase()))
  ),[cat,q]);

  const add = (p, originEl) => {
    setLines(L => {
      const ex = L.find(l=>l.id===p.id);
      return ex ? L.map(l=>l.id===p.id?{...l,qty:l.qty+1}:l) : [...L, {...p, qty:1}];
    });
    if (originEl && cartRef.current) fly(originEl, cartRef.current);
  };
  const dec = (id) => setLines(L => L.flatMap(l => l.id===id ? (l.qty<=1?[]:[{...l,qty:l.qty-1}]) : [l]));
  const inc = (id) => setLines(L => L.map(l=>l.id===id?{...l,qty:l.qty+1}:l));
  const rm  = (id) => setLines(L => L.filter(l=>l.id!==id));

  const subtotal = lines.reduce((s,l)=>s+l.price*l.qty,0);
  const discount = 0;
  const vat = Math.round((subtotal-discount)*0.15);
  const total = subtotal - discount + vat;

  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="pos" />
      {/* Center / product area */}
      <div style={{flex:1,minWidth:0,display:"flex",flexDirection:"column"}}>
        <AppBar
          title="POS Sale"
          subtitle="Counter 01 · Shift opened 08:32"
          right={
            <div style={{display:"flex",gap:8,alignItems:"center"}}>
              <button className="btn btn-secondary btn-sm"><Icon.refresh size={14}/> Hold</button>
              <button className="btn btn-secondary btn-sm" onClick={()=>onGoTo&&onGoTo("sales-list")}><Icon.receipt size={14}/> Sales</button>
              <button className="btn btn-secondary btn-sm" onClick={onTheme} title="Theme">
                {theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}
              </button>
            </div>
          }
        />

        {/* Customer + search bar */}
        <div style={{padding:"14px 22px 0",display:"flex",gap:10,alignItems:"center",flexWrap:"nowrap",overflow:"hidden"}}>
          <div className="card" style={{display:"flex",alignItems:"center",gap:8,padding:"8px 10px",borderRadius:12,flexShrink:0,maxWidth:260}}>
            <div style={{width:32,height:32,borderRadius:8,background:"var(--amber-soft)",color:"var(--amber-press)",display:"flex",alignItems:"center",justifyContent:"center",fontWeight:700,fontFamily:"var(--mono)",fontSize:12,flexShrink:0}}>RS</div>
            <div style={{flex:1,minWidth:0}}>
              <div style={{fontSize:10,color:"var(--muted)",letterSpacing:".06em",textTransform:"uppercase",fontWeight:600}}>Customer</div>
              <div style={{fontSize:12,fontWeight:600,whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis"}}>{customer.name}</div>
            </div>
            <button className="btn btn-ghost btn-sm" style={{padding:0,width:28,height:28,flexShrink:0}}><Icon.chev_d size={14}/></button>
          </div>

          <div className="field" style={{flex:1, minWidth:0, background:"var(--raised)"}}>
            <Icon.search size={16}/>
            <input value={q} onChange={e=>setQ(e.target.value)} placeholder="Search product or SKU…"/>
            <span style={{fontSize:10,color:"var(--muted)",fontFamily:"var(--mono)",background:"var(--raised-2)",padding:"3px 6px",borderRadius:4,border:"1px solid var(--hairline)"}}>⌘K</span>
          </div>
          <button className="btn btn-secondary btn-sm" style={{flexShrink:0}} onClick={()=>onGoTo&&onGoTo("scanner")}><Icon.barcode size={14}/> Scan</button>
          <button className="btn btn-secondary btn-sm" style={{flexShrink:0}}><Icon.plus size={14}/> New</button>
        </div>

        {/* Category chips */}
        <div style={{padding:"14px 22px 6px",display:"flex",gap:8,overflowX:"auto"}} className="scroll">
          {CATS.map(c=>(
            <button key={c} className={"chip"+(cat===c?" active":"")} onClick={()=>setCat(c)}>
              {c==="All" ? <span style={{display:"inline-flex",width:6,height:6,borderRadius:99,background:cat==="All"?"var(--amber)":"var(--hairline)"}}/> : null}
              {c}
              <span className="num" style={{opacity:.7,fontSize:11}}>
                {c==="All"?PRODUCTS.length:PRODUCTS.filter(p=>p.cat===c).length}
              </span>
            </button>
          ))}
        </div>

        {/* Product grid */}
        <div className="scroll" style={{flex:1,overflow:"auto",padding:"10px 22px 22px"}}>
          <div className="reveal" style={{display:"grid",gridTemplateColumns:"repeat(4, 1fr)",gap:14}}>
            {visible.map(p=>(
              <ProductCard key={p.id} p={p} onAdd={add}/>
            ))}
          </div>
        </div>
      </div>

      {/* Ticket panel — right side */}
      <div ref={cartRef} style={{width:380,display:"flex",flexDirection:"column",background:"var(--surface)",borderLeft:"1px solid var(--hairline)",backgroundImage:"var(--grain)",backgroundBlendMode:"multiply"}}>
        <div style={{padding:"18px 18px 10px",borderBottom:"1px solid var(--hairline)"}}>
          <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
            <div>
              <div className="eyebrow">Current Ticket</div>
              <div style={{fontFamily:"var(--mono)",fontSize:15,fontWeight:700,marginTop:2}}>S-00010</div>
            </div>
            <span className="badge badge-amber">● Open</span>
          </div>
        </div>

        {lines.length===0 ? (
          <div style={{flex:1,display:"flex",flexDirection:"column",alignItems:"center",justifyContent:"center",padding:32,gap:10,color:"var(--muted)"}}>
            <div style={{width:74,height:74,borderRadius:18,background:"var(--raised-2)",display:"flex",alignItems:"center",justifyContent:"center",border:"1px dashed var(--hairline)"}}>
              <Icon.cart size={30}/>
            </div>
            <div style={{fontWeight:600,color:"var(--ink)"}}>Ticket is empty</div>
            <div style={{fontSize:12,textAlign:"center",maxWidth:240,lineHeight:1.4}}>Tap a product, scan a barcode, or press <span style={{fontFamily:"var(--mono)",border:"1px solid var(--hairline)",padding:"1px 5px",borderRadius:4,fontSize:10}}>F2</span> for quick search.</div>
          </div>
        ) : (
          <div className="scroll" style={{flex:1,overflow:"auto"}}>
            {lines.map(l => (
              <TicketLine key={l.id} line={l}
                onDec={()=>dec(l.id)} onInc={()=>inc(l.id)} onRm={()=>rm(l.id)}/>
            ))}
          </div>
        )}

        {/* Totals */}
        <div style={{padding:"14px 18px",borderTop:"1px solid var(--hairline)",background:"var(--surface)"}}>
          <Row label="Subtotal" value={`Rs ${subtotal.toLocaleString()}`}/>
          <Row label="Discount" value="— Rs 0" muted/>
          <Row label="VAT (15%, incl.)" value={`Rs ${vat.toLocaleString()}`} muted/>

          <div style={{margin:"10px 0",borderTop:"1px dashed var(--hairline)"}}/>

          <div style={{display:"flex",alignItems:"baseline",justifyContent:"space-between"}}>
            <span style={{fontSize:13,fontWeight:600,letterSpacing:".06em",textTransform:"uppercase",color:"var(--muted)"}}>Total</span>
            <span className="num" style={{fontSize:34,fontWeight:800,letterSpacing:"-0.02em"}}>
              Rs <CountUp value={total} decimals={0}/>
            </span>
          </div>

          <div style={{display:"flex",gap:8,marginTop:14}}>
            <button className="btn btn-secondary" style={{flex:1}} onClick={()=>setLines([])}>Clear</button>
            <button className="btn btn-secondary" style={{flex:1}}>Hold</button>
          </div>
          <button className="btn btn-primary btn-xl" style={{width:"100%",marginTop:8}} onClick={()=>onCharge({lines,total,subtotal,vat,customer})}>
            Charge Rs <CountUp value={total} decimals={0}/>
            <Icon.arrow_r size={20}/>
          </button>
        </div>
      </div>

      {flyLayer}
    </div>
  );
}

function Row({label,value,muted}) {
  return (
    <div style={{display:"flex",justifyContent:"space-between",alignItems:"baseline",padding:"3px 0"}}>
      <span style={{fontSize:13,color:muted?"var(--muted)":"var(--graphite)"}}>{label}</span>
      <span className="num" style={{fontSize:14,fontWeight:600,color:muted?"var(--muted)":"var(--ink)"}}>{value}</span>
    </div>
  );
}

/* ============================================================
   CHECKOUT
   ============================================================ */
function Checkout({ data, onComplete, onBack, theme, onTheme }) {
  const {lines, subtotal, vat, customer, total: baseTotal} = data || {lines:[],subtotal:0,vat:0,customer:{},total:0};
  const [discount, setDiscount] = uS(0);
  const [shipping, setShipping] = uS(0);
  const [pay, setPay] = uS("cash");
  const [received, setReceived] = uS(baseTotal);
  const [splits, setSplits] = uS([]); // {method, amount}

  const total = Math.max(0, subtotal - discount + vat + shipping);
  const rounded = Math.round(total / 5) * 5;
  const change = received - rounded;

  const pressKey = (k) => {
    setReceived(r=>{
      const s = String(r||0);
      if (k==="C") return 0;
      if (k==="←") return Number(s.slice(0,-1)||0);
      if (k===".") return r;
      return Number(s + k);
    });
  };

  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="pos"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0}}>
        <AppBar
          title="Checkout"
          subtitle={<>Invoice <span className="num" style={{fontWeight:600,color:"var(--ink)"}}>S-00010</span> · 26 May 2026 · 14:08</>}
          right={
            <div style={{display:"flex",gap:8}}>
              <button className="btn btn-secondary btn-sm" onClick={onBack}><Icon.close size={14}/> Cancel</button>
              <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
            </div>
          }
        />

        <div style={{flex:1,display:"grid",gridTemplateColumns:"1.05fr 1fr",gap:18,padding:18,overflow:"hidden"}}>
          {/* Left — items + charges */}
          <div className="card-machined" style={{display:"flex",flexDirection:"column",overflow:"hidden",padding:0}}>
            <div style={{padding:"16px 18px 10px",borderBottom:"1px solid var(--hairline-2)"}}>
              <div className="eyebrow">Bill to</div>
              <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginTop:6}}>
                <div>
                  <div style={{fontWeight:700,fontSize:15}}>{customer.name||"Ravi Soobramoney"}</div>
                  <div style={{fontSize:12,color:"var(--muted)",fontFamily:"var(--mono)"}}>{customer.phone||"+230 5712 4408"}</div>
                </div>
                <button className="btn btn-ghost btn-sm">Change</button>
              </div>
            </div>

            <div className="scroll" style={{flex:1,overflow:"auto",padding:"6px 6px"}}>
              {lines.map(l => (
                <div key={l.id} style={{display:"flex",alignItems:"center",gap:10,padding:"10px 12px",borderBottom:"1px solid var(--hairline-2)"}}>
                  <div style={{width:38,height:38,borderRadius:8,background:"var(--raised-2)",overflow:"hidden",display:"flex",alignItems:"center",justifyContent:"center"}}>
                    <ProductTile kind={l.kind} size={36}/>
                  </div>
                  <div style={{flex:1,minWidth:0}}>
                    <div style={{fontSize:13,fontWeight:600,whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis"}}>{l.name}</div>
                    <div style={{fontSize:11,color:"var(--muted)",fontFamily:"var(--mono)"}}>{l.sku} · {l.qty} × Rs {l.price.toLocaleString()}</div>
                  </div>
                  <div className="num" style={{fontSize:14,fontWeight:700}}>Rs {(l.qty*l.price).toLocaleString()}</div>
                </div>
              ))}
            </div>

            {/* Charges */}
            <div style={{padding:14,borderTop:"1px solid var(--hairline)",display:"grid",gridTemplateColumns:"1fr 1fr",gap:10,background:"var(--raised-2)"}}>
              <FieldNum label="Discount (flat Rs)" v={discount} setV={setDiscount}/>
              <FieldNum label="Shipping" v={shipping} setV={setShipping}/>
              <FieldNum label="VAT (15%, calc)" v={vat} readOnly/>
              <FieldRO label="Rounding" v="Rs 0"/>
            </div>

            {/* Total */}
            <div style={{padding:"16px 18px",borderTop:"1px solid var(--hairline)",background:"var(--surface)"}}>
              <div style={{display:"flex",justifyContent:"space-between",fontSize:13,color:"var(--muted)"}}>
                <span>Subtotal</span><span className="num">Rs {subtotal.toLocaleString()}</span>
              </div>
              <div style={{display:"flex",justifyContent:"space-between",alignItems:"baseline",marginTop:4}}>
                <span style={{fontSize:13,letterSpacing:".06em",textTransform:"uppercase",fontWeight:600,color:"var(--muted)"}}>Rounded total</span>
                <span className="num" style={{fontSize:32,fontWeight:800,letterSpacing:"-0.02em"}}>Rs <CountUp value={rounded} decimals={0}/></span>
              </div>
            </div>
          </div>

          {/* Right — Payment + keypad */}
          <div className="card-machined" style={{display:"flex",flexDirection:"column",overflow:"hidden",padding:0}}>
            <div style={{padding:"14px 18px 10px",borderBottom:"1px solid var(--hairline-2)"}}>
              <div className="eyebrow">Payment type</div>
              <div style={{display:"grid",gridTemplateColumns:"repeat(4, 1fr)",gap:8,marginTop:8}}>
                {[
                  {id:"cash",l:"Cash",ic:"cash"},
                  {id:"card",l:"Card",ic:"card"},
                  {id:"mobile",l:"Juice",ic:"mobile"},
                  {id:"credit",l:"Credit",ic:"wallet"},
                ].map(o=>{
                  const on = pay===o.id;
                  const IcC = Icon[o.ic];
                  return (
                    <button key={o.id} onClick={()=>setPay(o.id)} style={{
                      border:`1.5px solid ${on?"var(--ink)":"var(--hairline)"}`,
                      background: on?"var(--ink)":"var(--raised)",
                      color: on?"var(--surface)":"var(--ink)",
                      borderRadius: 12, padding:"10px 8px", cursor:"pointer",
                      display:"flex",flexDirection:"column",alignItems:"center",gap:4,
                      transition:"all .15s ease"
                    }}>
                      <IcC size={20}/>
                      <span style={{fontSize:12,fontWeight:700}}>{o.l}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Received + change */}
            <div style={{padding:"14px 18px",display:"grid",gridTemplateColumns:"1fr 1fr",gap:12,borderBottom:"1px solid var(--hairline-2)"}}>
              <div>
                <div className="eyebrow">Received</div>
                <div className="num" style={{fontSize:30,fontWeight:800,letterSpacing:"-0.02em",marginTop:4}}>Rs {received.toLocaleString()}</div>
              </div>
              <div>
                <div className="eyebrow">{change>=0?"Change":"Amount due"}</div>
                <div className="num" style={{fontSize:30,fontWeight:800,letterSpacing:"-0.02em",marginTop:4,color: change>=0?"var(--emerald)":"var(--crimson)"}}>
                  Rs {Math.abs(change).toLocaleString()}
                </div>
              </div>
            </div>

            {/* Keypad */}
            <div style={{flex:1,padding:14,display:"grid",gridTemplateColumns:"repeat(4, 1fr)",gridAutoRows:"1fr",gap:8}}>
              {["1","2","3","C","4","5","6","←","7","8","9",".","00","0",".0"].slice(0,12).map((k,i)=>{
                const danger = k==="C" || k==="←";
                return (
                  <button key={i} onClick={()=>pressKey(k)} className="card-machined" style={{
                    fontFamily:"var(--mono)", fontSize:24, fontWeight:700,
                    border:"1px solid var(--hairline)", cursor:"pointer", 
                    background: danger? "var(--raised-2)" : "var(--raised)",
                    color: danger? "var(--crimson)" : "var(--ink)",
                    transition:"transform .08s ease"
                  }} onPointerDown={e=>e.currentTarget.style.transform="translateY(2px)"} onPointerUp={e=>e.currentTarget.style.transform=""}>
                    {k}
                  </button>
                );
              })}
            </div>

            {/* Quick amount + Confirm */}
            <div style={{padding:14,borderTop:"1px solid var(--hairline)",background:"var(--raised-2)"}}>
              <div style={{display:"flex",gap:6,marginBottom:10}}>
                {[rounded,500,1000,2000,5000].map((v,i)=>(
                  <button key={i} className="chip" onClick={()=>setReceived(v)}>
                    {i===0?"Exact":""} <span className="num">Rs {v.toLocaleString()}</span>
                  </button>
                ))}
              </div>
              <button className="btn btn-primary btn-xl" style={{width:"100%"}} onClick={()=>onComplete({...data, total: rounded, received, change, pay})}>
                <Icon.check size={20}/> Complete sale · Rs <CountUp value={rounded} decimals={0}/>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function FieldNum({label,v,setV,readOnly}) {
  return (
    <div>
      <div className="label">{label}</div>
      <div className="field" style={{height:42}}>
        <span style={{color:"var(--muted)",fontSize:13}}>Rs</span>
        <input className="num" style={{textAlign:"right",fontWeight:600}}
               readOnly={readOnly}
               value={v} onChange={e=>setV&&setV(Number(e.target.value)||0)}/>
      </div>
    </div>
  );
}
function FieldRO({label,v}) {
  return (<div><div className="label">{label}</div><div className="field" style={{height:42,background:"var(--raised-2)"}}><span className="num" style={{flex:1,textAlign:"right",fontWeight:600,color:"var(--muted)"}}>{v}</span></div></div>);
}

/* ============================================================
   RECEIPT / COMPLETE
   ============================================================ */
function Receipt({ data, onNew, onBack, theme, onTheme }) {
  const {lines=[], subtotal=0, vat=0, customer={}, total=0, received=0, change=0, pay="cash"} = data||{};
  const [showStamp, setShow] = uS(false);
  uE(()=>{ const t=setTimeout(()=>setShow(true), 250); return ()=>clearTimeout(t); },[]);
  return (
    <div style={{display:"flex",height:"100%",background:"var(--bg)"}}>
      <NavRail active="pos"/>
      <div style={{flex:1,display:"flex",flexDirection:"column",minWidth:0}}>
        <AppBar title="Sale complete" subtitle={<>Invoice <span className="num">S-00010</span> · paid in {pay}</>}
          right={
            <div style={{display:"flex",gap:8}}>
              <button className="btn btn-secondary btn-sm" onClick={onTheme}>{theme==="light"?<Icon.moon size={14}/>:<Icon.sun size={14}/>}</button>
              <button className="btn btn-secondary" onClick={onBack}><Icon.arrow_r size={16} style={{transform:"rotate(180deg)"}}/> Back to POS</button>
            </div>}
        />
        <div style={{flex:1,display:"grid",gridTemplateColumns:"1fr 1fr",gap:20,padding:24,overflow:"hidden"}}>
          {/* Confirmation */}
          <div style={{display:"flex",flexDirection:"column",justifyContent:"center",gap:24,padding:"0 20px"}}>
            <div style={{display:"inline-flex",alignItems:"center",gap:10,color:"var(--emerald)",fontWeight:700,fontSize:13,letterSpacing:".06em",textTransform:"uppercase"}}>
              <span style={{width:8,height:8,borderRadius:99,background:"var(--emerald)"}}/> Payment confirmed
            </div>
            <h1 style={{margin:0,fontSize:60,fontWeight:800,letterSpacing:"-0.03em",lineHeight:.95}}>
              Rs <CountUp value={total} decimals={0}/>
              <div style={{fontSize:16,fontWeight:600,color:"var(--muted)",letterSpacing:0,marginTop:8,textTransform:"none"}}>
                received from {customer.name||"Walk-in"} · change Rs {Math.max(0,change).toLocaleString()}
              </div>
            </h1>
            <div style={{display:"flex",gap:10,flexWrap:"wrap"}}>
              <button className="btn btn-primary btn-lg"><Icon.print size={18}/> Print receipt</button>
              <button className="btn btn-secondary btn-lg"><Icon.share size={18}/> SMS / WhatsApp</button>
              <button className="btn btn-secondary btn-lg"><Icon.download size={18}/> PDF</button>
            </div>
            <div className="card" style={{padding:16,display:"flex",alignItems:"center",gap:14,maxWidth:480}}>
              <div style={{width:46,height:46,borderRadius:12,background:"var(--amber-soft)",color:"var(--amber-press)",display:"flex",alignItems:"center",justifyContent:"center"}}><Icon.cart size={22}/></div>
              <div style={{flex:1}}>
                <div style={{fontSize:13,fontWeight:700}}>New ticket S-00011 ready</div>
                <div style={{fontSize:11,color:"var(--muted)",fontFamily:"var(--mono)"}}>Auto-incremented · scan to begin</div>
              </div>
              <button className="btn btn-primary btn-sm" onClick={onNew}>Start <Icon.arrow_r size={14}/></button>
            </div>
          </div>

          {/* Receipt paper preview */}
          <div style={{display:"flex",alignItems:"center",justifyContent:"center",position:"relative",overflow:"hidden"}}>
            <div style={{
              width: 320, maxHeight:"100%", background:"#FBF7EE", color:"#14110C",
              padding: "22px 22px 30px", borderRadius:8,
              boxShadow:"0 25px 60px rgba(0,0,0,0.20), 0 6px 18px rgba(0,0,0,0.10)",
              backgroundImage:"var(--grain)", backgroundBlendMode:"multiply",
              position:"relative",
              fontFamily:"var(--mono)",
              animation:"slide-up .6s cubic-bezier(.2,.7,.2,1) both",
              clipPath:"polygon(0 0, 100% 0, 100% calc(100% - 12px), 96% 100%, 92% calc(100% - 8px), 88% 100%, 84% calc(100% - 8px), 80% 100%, 76% calc(100% - 8px), 72% 100%, 68% calc(100% - 8px), 64% 100%, 60% calc(100% - 8px), 56% 100%, 52% calc(100% - 8px), 48% 100%, 44% calc(100% - 8px), 40% 100%, 36% calc(100% - 8px), 32% 100%, 28% calc(100% - 8px), 24% 100%, 20% calc(100% - 8px), 16% 100%, 12% calc(100% - 8px), 8% 100%, 4% calc(100% - 8px), 0 100%)"
            }}>
              <div style={{textAlign:"center",fontFamily:"var(--display)",fontWeight:800,fontSize:16,letterSpacing:"-0.01em"}}>QUINCAILLERIE RB TRADING</div>
              <div style={{textAlign:"center",fontSize:10,color:"#5b5246",marginTop:2}}>Royal Rd, Curepipe · BRN C20177445</div>
              <div style={{textAlign:"center",fontSize:10,color:"#5b5246"}}>VAT: VAT20188822 · +230 670 4408</div>
              <div style={{borderTop:"1px dashed #c8bda5",margin:"12px 0"}}/>
              <div style={{display:"flex",justifyContent:"space-between",fontSize:11}}>
                <span>Invoice</span><span>S-00010</span>
              </div>
              <div style={{display:"flex",justifyContent:"space-between",fontSize:11}}>
                <span>Date</span><span>26 May 2026 · 14:08</span>
              </div>
              <div style={{display:"flex",justifyContent:"space-between",fontSize:11}}>
                <span>Cashier</span><span>S. Khan</span>
              </div>
              <div style={{display:"flex",justifyContent:"space-between",fontSize:11}}>
                <span>Customer</span><span>{customer.name?.split(" ")[0]||"Walk-in"}</span>
              </div>
              <div style={{borderTop:"1px dashed #c8bda5",margin:"12px 0"}}/>
              {lines.map(l=>(
                <div key={l.id} style={{marginBottom:6}}>
                  <div style={{fontSize:11,fontFamily:"var(--display)",fontWeight:600}}>{l.name}</div>
                  <div style={{display:"flex",justifyContent:"space-between",fontSize:11}}>
                    <span>{l.qty} × {l.price.toLocaleString()}</span>
                    <span>{(l.qty*l.price).toLocaleString()}</span>
                  </div>
                </div>
              ))}
              <div style={{borderTop:"1px dashed #c8bda5",margin:"10px 0"}}/>
              <Rr l="Subtotal" v={subtotal}/>
              <Rr l="VAT 15%" v={vat}/>
              <Rr l="Discount" v={0}/>
              <div style={{borderTop:"1px solid #14110C",margin:"6px 0"}}/>
              <Rr l="TOTAL" v={total} big/>
              <Rr l={"Paid · "+pay} v={received}/>
              <Rr l="Change" v={Math.max(0,change)}/>
              <div style={{borderTop:"1px dashed #c8bda5",margin:"10px 0"}}/>
              <div style={{textAlign:"center",fontSize:10,color:"#5b5246",lineHeight:1.5}}>Goods sold are not refundable. Thank you for shopping with us.</div>
              <div style={{textAlign:"center",fontFamily:"var(--display)",fontWeight:600,fontSize:11,marginTop:8}}>powered by NexaPOS</div>
              <div style={{display:"flex",justifyContent:"center",marginTop:10}}>
                <svg width="200" height="40" viewBox="0 0 200 40">
                  {Array.from({length:60}).map((_,i)=>(
                    <rect key={i} x={i*3.3} y="0" width={(i%3===0?2:i%2?1.4:0.8)} height="40" fill="#14110C"/>
                  ))}
                </svg>
              </div>
              <div className="num" style={{textAlign:"center",fontSize:10,marginTop:4,letterSpacing:".12em"}}>S-00010-26052026</div>

              {/* Stamp */}
              {showStamp && (
                <div style={{
                  position:"absolute", top: 90, right: 24,
                  border: "3px solid var(--emerald)", color:"var(--emerald)",
                  padding:"6px 14px", borderRadius:6,
                  fontFamily:"var(--display)", fontWeight:800, letterSpacing:".08em", fontSize:18,
                  transform:"rotate(-12deg)", animation:"stamp .55s cubic-bezier(.2,1.3,.4,1) both",
                  background:"rgba(255,255,255,0.4)"
                }}>PAID</div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
function Rr({l,v,big}) {
  return (
    <div style={{display:"flex",justifyContent:"space-between",fontSize: big?14:11.5, fontWeight: big?800:500, padding:"2px 0"}}>
      <span style={{fontFamily:"var(--display)"}}>{l}</span>
      <span>Rs {Number(v).toLocaleString()}</span>
    </div>
  );
}

Object.assign(window, { POSSale, Checkout, Receipt, PRODUCTS });
