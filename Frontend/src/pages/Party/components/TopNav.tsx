import React, { useState, useEffect, MouseEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import './topnav.css';
import jwtAxios from '@/utils/JWTUtil';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

interface ModalProps {
  isVisible: boolean;
  role: string;
  state: string;
  partyId: string | undefined;
  expectedCost: number;
  currentParticipants: number;
  onClose: () => void;
}

interface INavProps {
  role: string;
  state: string;
  partyId: string | undefined;
  expectedCost: number;
  currentParticipants: number;
}

const Modal: React.FC<ModalProps & { fetchData: () => void }> = ({
  isVisible,
  onClose,
  role,
  state,
  partyId,
  expectedCost,
  currentParticipants,
  fetchData,
}) => {
  if (!isVisible) return null;

  const nav = useNavigate();
  // Handle outside click to close modal
  const handleOutsideClick = (e: MouseEvent<HTMLDivElement>) => {
    if ((e.target as HTMLDivElement).id === 'modal-overlay') {
      onClose(); // Close modal on outside click
    }
  };

  const successParty = () => {
    jwtAxios
      .post(
        `api/parties/${partyId}/success`,
        {},
        {
          params: {
            expected_cost: expectedCost / currentParticipants,
          },
        },
      )
      .then(res => {
        console.log(res.data);
        if (res.data.status === 204) {
          toast.success(`${res.data.message} 선 차감된 금액: ${res.data.data.prepaidCost}`, {
            position: 'top-center',
          });
        }
        fetchData();
      })
      .catch(err => {
        console.error(err);
        toast.error(`${err.response.data.message} ${err.response.data.reason}`, {
          position: 'top-center',
        });
      });
  };

  function deleteParty() {
    jwtAxios
      .delete(`api/party-boards/${partyId}`, {})
      .then(res => {
        console.log(res.data);
        if (res.data.status === 204) {
          toast.success(`${res.data.message}`, { position: 'top-center' });
          nav('/home', { replace: true });
        }
        fetchData();
      })
      .catch(err => {
        console.log(err);
      });
  }

  return (
    <div
      id='modal-overlay'
      className='fixed inset-0 bg-black bg-opacity-50 flex justify-center items-end'
      onClick={handleOutsideClick}>
      <div
        className='bg-white p-4 w-full md:max-w-md mx-auto rounded shadow-lg flex flex-col space-y-2'
        onClick={e => e.stopPropagation()}>
        {role === 'ORGANIZER' && state === 'GATHERING' && (
          <button onClick={successParty} className='btn btn-ghost text-blue-500'>
            팟 확정하기
          </button>
        )}
        <button className='btn btn-ghost text-black'>팟 수정하기</button>
        <button onClick={deleteParty} className='btn btn-ghost text-red-500'>
          팟 삭제하기
        </button>
        <button onClick={onClose} className='btn btn-ghost text-gray-500'>
          취소
        </button>
      </div>
    </div>
  );
};

const TopNav: React.FC<INavProps & { fetchData: () => void }> = ({
  role,
  state,
  partyId,
  expectedCost,
  currentParticipants,
  fetchData,
}) => {
  const [modalVisible, setModalVisible] = useState(false);
  const nav = useNavigate();

  useEffect(() => {
    // Lock or unlock body scroll based on modal visibility
    document.body.style.overflow = modalVisible ? 'hidden' : 'auto';
  }, [modalVisible]);

  const goBack = () => {
    nav(-1);
  };

  const toggleModal = () => {
    setModalVisible(!modalVisible);
  };

  const closeModal = () => {
    setModalVisible(false);
  };

  return (
    <div className='navbar bg-base-100 justify-between'>
      <div className='flex-none'>
        <button onClick={goBack} className='btn btn-square btn-ghost'>
          <svg
            xmlns='http://www.w3.org/2000/svg'
            fill='none'
            viewBox='0 0 24 24'
            className='inline-block w-5 h-5 stroke-current'>
            <path
              strokeLinecap='round'
              strokeLinejoin='round'
              strokeWidth='2'
              d='M4 6h16M4 12h16M4 18h16'></path>
          </svg>
        </button>
      </div>
      {role == 'ORGANIZER' && (
        <div className='flex-none'>
          <button onClick={toggleModal} className='btn btn-square btn-ghost'>
            <svg
              xmlns='http://www.w3.org/2000/svg'
              fill='none'
              viewBox='0 0 24 24'
              className='inline-block w-5 h-5 stroke-current'>
              <path
                strokeLinecap='round'
                strokeLinejoin='round'
                strokeWidth='2'
                d='M5 12h.01M12 12h.01M19 12h.01M6 12a1 1 0 11-2 0 1 1 0 012 0zm7 0 a1 1 0 11-2 0 1 1 0 012 0zm7 0 a1 1 0 11-2 0 1 1 0 012 0z'></path>
            </svg>
          </button>
        </div>
      )}
      {modalVisible && (
        <Modal
          role={role}
          state={state}
          partyId={partyId}
          expectedCost={expectedCost}
          currentParticipants={currentParticipants}
          fetchData={fetchData}
          isVisible={modalVisible}
          onClose={closeModal}
        />
      )}
    </div>
  );
};

export default TopNav;